package org.apache.hadoop.net;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.ConfigurationFactory;
import org.apache.hadoop.util.StopWatch;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.InetAddresses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Name;
import org.xbill.DNS.ResolverConfig;

public class HostResolverUtil {

  private static final Logger LOG = LoggerFactory.getLogger(HostResolver.class);

  // controls whether buildTokenService will use an ip or host/ip as given
  // by the user
  @VisibleForTesting
  public static boolean useIpForTokenService;

  @VisibleForTesting
  static HostResolver hostResolver;

  private static boolean logSlowLookups;

  private static int slowLookupThresholdMs;

  static {
    setConfigurationInternal(ConfigurationFactory.newInstance());
  }

  @InterfaceAudience.Public
  @InterfaceStability.Evolving
  public static void setConfiguration(Configuration conf) {
    LOG.info("Updating Configuration");
    setConfigurationInternal(conf);
  }


  private static void setConfigurationInternal(Configuration conf) {
    logSlowLookups = conf.getBoolean(
        "hadoop.security.dns.log-slow-lookups.enabled",
        false);

    slowLookupThresholdMs = conf.getInt(
        "hadoop.security.dns.log-slow-lookups.threshold.ms",
        1000);

    boolean useIp = conf.getBoolean("hadoop.security.token.service.use_ip",
        false);
    setTokenServiceUseIp(useIp);
  }
  /**
   * For use only by tests and initialization
   */
  @InterfaceAudience.Private
  @VisibleForTesting
  public static void setTokenServiceUseIp(boolean flag) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Setting "
          + "hadoop.security.token.service.use_ip"
          + " to " + flag);
    }
    useIpForTokenService = flag;
    hostResolver = !useIpForTokenService
        ? new QualifiedHostResolver()
        : new StandardHostResolver();
  }

  /**
   * Resolves a host subject to the security requirements determined by
   * hadoop.security.token.service.use_ip. Optionally logs slow resolutions.
   *
   * @param hostname host or ip to resolve
   * @return a resolved host
   * @throws UnknownHostException if the host doesn't exist
   */
  @InterfaceAudience.Private
  public static InetAddress getByName(String hostname)
      throws UnknownHostException {
    if (logSlowLookups || LOG.isTraceEnabled()) {
      StopWatch lookupTimer = new StopWatch().start();
      InetAddress result = hostResolver.getByName(hostname);
      long elapsedMs = lookupTimer.stop().now(TimeUnit.MILLISECONDS);

      if (elapsedMs >= slowLookupThresholdMs) {
        LOG.warn("Slow name lookup for " + hostname + ". Took " + elapsedMs +
            " ms.");
      } else if (LOG.isTraceEnabled()) {
        LOG.trace("Name lookup for " + hostname + " took " + elapsedMs +
            " ms.");
      }
      return result;
    } else {
      return hostResolver.getByName(hostname);
    }
  }

  public interface HostResolver {
    InetAddress getByName(String host) throws UnknownHostException;
  }

  /**
   * Uses standard java host resolution
   */
  public static class StandardHostResolver implements HostResolver {
    @Override
    public InetAddress getByName(String host) throws UnknownHostException {
      return InetAddress.getByName(host);
    }
  }

  /**
   * This an alternate resolver with important properties that the standard
   * java resolver lacks:
   * 1) The hostname is fully qualified.  This avoids security issues if not
   * all hosts in the cluster do not share the same search domains.  It
   * also prevents other hosts from performing unnecessary dns searches.
   * In contrast, InetAddress simply returns the host as given.
   * 2) The InetAddress is instantiated with an exact host and IP to prevent
   * further unnecessary lookups.  InetAddress may perform an unnecessary
   * reverse lookup for an IP.
   * 3) A call to getHostName() will always return the qualified hostname, or
   * more importantly, the IP if instantiated with an IP.  This avoids
   * unnecessary dns timeouts if the host is not resolvable.
   * 4) Point 3 also ensures that if the host is re-resolved, ex. during a
   * connection re-attempt, that a reverse lookup to host and forward
   * lookup to IP is not performed since the reverse/forward mappings may
   * not always return the same IP.  If the client initiated a connection
   * with an IP, then that IP is all that should ever be contacted.
   * <p>
   * NOTE: this resolver is only used if:
   * hadoop.security.token.service.use_ip=false
   */
  public static class QualifiedHostResolver implements HostResolver {
    private List<String> searchDomains = new ArrayList<>();

    {
      ResolverConfig resolverConfig = ResolverConfig.getCurrentConfig();
      Name[] names = resolverConfig.searchPath();
      if (names != null) {
        for (Name name : names) {
          searchDomains.add(name.toString());
        }
      }
    }


    /**
     * Create an InetAddress with a fully qualified hostname of the given
     * hostname.  InetAddress does not qualify an incomplete hostname that
     * is resolved via the domain search list.
     * {@link InetAddress#getCanonicalHostName()} will fully qualify the
     * hostname, but it always return the A record whereas the given hostname
     * may be a CNAME.
     *
     * @param host a hostname or ip address
     * @return InetAddress with the fully qualified hostname or ip
     * @throws UnknownHostException if host does not exist
     */
    @Override
    public InetAddress getByName(String host) throws UnknownHostException {
      InetAddress addr = null;

      if (InetAddresses.isInetAddress(host)) {
        // valid ip address. use it as-is
        addr = InetAddresses.forString(host);
        // set hostname
        addr = InetAddress.getByAddress(host, addr.getAddress());
      } else if (host.endsWith(".")) {
        // a rooted host ends with a dot, ex. "host."
        // rooted hosts never use the search path, so only try an exact lookup
        addr = getByExactName(host);
      } else if (host.contains(".")) {
        // the host contains a dot (domain), ex. "host.domain"
        // try an exact host lookup, then fallback to search list
        addr = getByExactName(host);
        if (addr == null) {
          addr = getByNameWithSearch(host);
        }
      } else {
        // it's a simple host with no dots, ex. "host"
        // try the search list, then fallback to exact host
        InetAddress loopback = InetAddress.getByName(null);
        if (host.equalsIgnoreCase(loopback.getHostName())) {
          addr = InetAddress.getByAddress(host, loopback.getAddress());
        } else {
          addr = getByNameWithSearch(host);
          if (addr == null) {
            addr = getByExactName(host);
          }
        }
      }
      // unresolvable!
      if (addr == null) {
        throw new UnknownHostException(host);
      }
      return addr;
    }

    InetAddress getByExactName(String host) {
      InetAddress addr = null;
      // InetAddress will use the search list unless the host is rooted
      // with a trailing dot.  The trailing dot will disable any use of the
      // search path in a lower level resolver.  See RFC 1535.
      String fqHost = host;
      if (!fqHost.endsWith(".")) fqHost += ".";
      try {
        addr = getInetAddressByName(fqHost);
        // can't leave the hostname as rooted or other parts of the system
        // malfunction, ex. kerberos principals are lacking proper host
        // equivalence for rooted/non-rooted hostnames
        addr = InetAddress.getByAddress(host, addr.getAddress());
      } catch (UnknownHostException e) {
        // ignore, caller will throw if necessary
      }
      return addr;
    }

    InetAddress getByNameWithSearch(String host) {
      InetAddress addr = null;
      if (host.endsWith(".")) { // already qualified?
        addr = getByExactName(host);
      } else {
        for (String domain : searchDomains) {
          String dot = !domain.startsWith(".") ? "." : "";
          addr = getByExactName(host + dot + domain);
          if (addr != null) break;
        }
      }
      return addr;
    }

    // implemented as a separate method to facilitate unit testing
    InetAddress getInetAddressByName(String host) throws UnknownHostException {
      return InetAddress.getByName(host);
    }

    void setSearchDomains(String ... domains) {
      searchDomains = Arrays.asList(domains);
    }
  }

}
