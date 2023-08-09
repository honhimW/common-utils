package io.github.honhimw;

import java.net.*;
import java.util.Enumeration;

/**
 * 获取本机IP
 * @author hon_him
 * @since 2022-05-31
 */
@SuppressWarnings("unused")
public class IpUtils {

    private static String IPV4;
    private static String IPV6;

    private IpUtils() {
    }

    public static String localIPv4() {
        if (IPV4 == null) {
            IPV4 = getIP(Inet4Address.class);
        }
        return IPV4;
    }

    public static String localIPv6() {
        if (IPV6 == null) {
            IPV6 = getIP(Inet6Address.class);
        }
        return IPV6;
    }

    private static String getIP(Class<? extends InetAddress> type) {
        String localip = null;// 本地IP，如果没有配置外网IP则返回它
        String netip = null;// 外网IP
        try {
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress inetAddress = null;
            // 是否找到外网IP
            out: while (netInterfaces.hasMoreElements()) {
                NetworkInterface ni = netInterfaces.nextElement();
                Enumeration<InetAddress> address = ni.getInetAddresses();
                while (address.hasMoreElements()) {
                    inetAddress = address.nextElement();
                    if (!inetAddress.isSiteLocalAddress() && !inetAddress.isLoopbackAddress()
                        && type.isAssignableFrom(inetAddress.getClass())) {// 外网IP
                        netip = inetAddress.getHostAddress();
                        break out;
                    } else if (inetAddress.isSiteLocalAddress() && !inetAddress.isLoopbackAddress()
                        && type.isAssignableFrom(inetAddress.getClass())) {// 内网IP
                        localip = inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        if (netip != null && !"".equals(netip)) {
            return netip;
        } else {
            return localip;
        }
    }

    public static String firstLocalIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ignored) {
            return null;
        }
    }


}
