package org.apache.hadoop.net;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.hadoop.util.StopWatch;

public class NetUtils {

  private static Map<String, String> hostToResolved =
      new HashMap<String, String>();

  public static InetSocketAddress createSocketAddr(String target) {
    return createSocketAddr(target, -1);
  }

  /**
   * Util method to build socket addr from either:
   *   {@literal <host>}
   *   {@literal <host>:<port>}
   *   {@literal <fs>://<host>:<port>/<path>}
   */
  public static InetSocketAddress createSocketAddr(String target,
      int defaultPort) {
    return createSocketAddr(target, defaultPort, null);
  }
  /**
   * Create an InetSocketAddress from the given target string and
   * default port. If the string cannot be parsed correctly, the
   * <code>configName</code> parameter is used as part of the
   * exception message, allowing the user to better diagnose
   * the misconfiguration.
   *
   * @param target a string of either "host" or "host:port"
   * @param defaultPort the default port if <code>target</code> does not
   *                    include a port number
   * @param configName the name of the configuration from which
   *                   <code>target</code> was loaded. This is used in the
   *                   exception message in the case that parsing fails.
   */
  public static InetSocketAddress createSocketAddr(String target,
      int defaultPort,
      String configName) {
    String helpText = "";
    if (configName != null) {
      helpText = " (configuration property '" + configName + "')";
    }
    if (target == null) {
      throw new IllegalArgumentException("Target address cannot be null." +
          helpText);
    }
    target = target.trim();
    boolean hasScheme = target.contains("://");
    URI uri = null;
    try {
      uri = hasScheme ? URI.create(target) : URI.create("dummyscheme://"+target);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Does not contain a valid host:port authority: " + target + helpText
      );
    }

    String host = uri.getHost();
    int port = uri.getPort();
    if (port == -1) {
      port = defaultPort;
    }
    String path = uri.getPath();

    if ((host == null) || (port < 0) ||
        (!hasScheme && path != null && !path.isEmpty()))
    {
      throw new IllegalArgumentException(
          "Does not contain a valid host:port authority: " + target + helpText
      );
    }
    return createSocketAddrForHost(host, port);
  }

  /**
   * Create a socket address with the given host and port.  The hostname
   * might be replaced with another host that was set via
   * hadoop.security.token.service.use_ip will determine whether the
   * standard java host resolver is used, or if the fully qualified resolver
   * is used.
   * @param host the hostname or IP use to instantiate the object
   * @param port the port number
   * @return InetSocketAddress
   */
  public static InetSocketAddress createSocketAddrForHost(String host, int port) {
    String staticHost = getStaticResolution(host);
    String resolveHost = (staticHost != null) ? staticHost : host;

    InetSocketAddress addr;
    try {
      InetAddress iaddr = HostResolverUtil.getByName(resolveHost);
      // if there is a static entry for the host, make the returned
      // address look like the original given host
      if (staticHost != null) {
        iaddr = InetAddress.getByAddress(host, iaddr.getAddress());
      }
      addr = new InetSocketAddress(iaddr, port);
    } catch (UnknownHostException e) {
      addr = InetSocketAddress.createUnresolved(host, port);
    }
    return addr;
  }

  /**
   * Retrieves the resolved name for the passed host. The resolved name must
   * have been set earlier using.
   * {@link NetUtils#addStaticResolution(String, String)}
   * @param host
   * @return the resolution
   */
  public static String getStaticResolution(String host) {
    synchronized (hostToResolved) {
      return hostToResolved.get(host);
    }
  }

  /**
   * Adds a static resolution for host. This can be used for setting up
   * hostnames with names that are fake to point to a well known host. For e.g.
   * in some testcases we require to have daemons with different hostnames
   * running on the same machine. In order to create connections to these
   * daemons, one can set up mappings from those hostnames to "localhost".
   * {@link NetUtils#getStaticResolution(String)} can be used to query for
   * the actual hostname..
   * @param host
   * @param resolvedName
   */
  public static void addStaticResolution(String host, String resolvedName) {
    synchronized (hostToResolved) {
      hostToResolved.put(host, resolvedName);
    }
  }
}
