import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * Javassist patch: the game froze about 5 s at launch (found 2026-10-09 in a
 * Flight Recorder profile: the client main thread sat in a socket connect).
 *
 * Cause: DecimationMod.postInit calls BackendClient.e(host, tcp, udp)
 * (deci.aP.a.e), which connects kryonet's Client to network.mcdecimation.net
 * with a 5000 ms timeout ON THE MAIN THREAD. That server is gone for good, so
 * every launch waited the full 5 s before the connect failed (the IOException
 * is caught by postInit). Later reconnect attempts run in their own threads
 * and reuse the last connect's timeout.
 *
 * Fix: the timeout becomes 300 ms, and kryonet's Client.connect (bundled in
 * Decimation.jar) is made to use it for the TCP connect, which it hardcoded to
 * 5000 ms (a first version that only shortened the timeout still waited 5 s).
 * Everything else is unchanged (the client
 * still registers its message types and its listener, the reconnect logic and
 * the "not connected" paths behave as before, only faster).
 *
 *   javac -cp tools/lib/javassist.jar --release 8 -d build/patch_backend tools/patches/PatchBackend.java
 *   java -cp tools/lib/javassist.jar:build/patch_backend PatchBackend <Decimation jar ending .jar> build/patch_backend/out <kryonet jar>
 *   (cd build/patch_backend/out && zip <jar> deci/aP/a.class com/esotericsoftware/kryonet/Client.class)
 * The kryonet classes ship inside Decimation.jar itself, so <kryonet jar> can
 * be the Decimation jar again. Patch from the ORIGINAL class.
 */
public class PatchBackend {
    public static void main(String[] a) throws Exception {
        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(a[0]);
        pool.insertClassPath(a[2]);
        CtClass client = pool.get("deci.aP.a");
        CtMethod connect = client.getMethod("e", "(Ljava/lang/String;II)V");
        final int[] done = {0};
        connect.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getClassName().equals("com.esotericsoftware.kryonet.Client") && m.getMethodName().equals("connect")) {
                    m.replace("{ $proceed(300, $2, $3, $4); }"); // was 5000 ms
                    done[0]++;
                }
            }
        });
        if (done[0] != 1) {
            throw new IllegalStateException("expected one Client.connect call, found " + done[0]);
        }
        client.writeFile(a[1]);

        // kryonet (bundled in Decimation.jar) ignores the timeout for the TCP
        // part: Client.connect(int, InetAddress, int, int) calls
        // TcpConnection.connect(selector, address, 5000). Use the given timeout.
        CtClass kryo = pool.get("com.esotericsoftware.kryonet.Client");
        kryo.addField(CtField.make("public static int tcpTimeout;", kryo), CtField.Initializer.constant(5000));
        CtMethod kconnect = kryo.getMethod("connect", "(ILjava/net/InetAddress;II)V");
        kconnect.insertBefore("{ tcpTimeout = $1; }");
        final int[] tcp = {0};
        kconnect.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getClassName().equals("com.esotericsoftware.kryonet.TcpConnection") && m.getMethodName().equals("connect")) {
                    m.replace("{ $proceed($1, $2, com.esotericsoftware.kryonet.Client.tcpTimeout); }");
                    tcp[0]++;
                }
            }
        });
        if (tcp[0] != 1) {
            throw new IllegalStateException("expected one TcpConnection.connect call, found " + tcp[0]);
        }
        kryo.writeFile(a[1]);
        System.out.println("patched deci.aP.a.e (BackendClient): backend connect timeout 5000 -> 300 ms, "
                           + "kryonet Client.connect: TCP connect uses that timeout");
    }
}
