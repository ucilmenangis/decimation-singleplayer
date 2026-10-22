import os, glob, zipfile, sys
P = sys.argv[1]
G = os.path.expanduser('~/.gradle/caches/modules-2/files-2.1')
SRV = os.path.join(os.path.dirname(P), 'prodsrv', 'server')
# client libraries of 1.7.10 (as the dev client uses them), by group/artifact/version
mc = ['org.lwjgl.lwjgl/lwjgl/2.9.4-nightly-20150209', 'org.lwjgl.lwjgl/lwjgl_util/2.9.4-nightly-20150209',
      'com.mojang/authlib/1.5.21', 'com.google.code.findbugs/jsr305/2.0.1', 'com.paulscode/codecjorbis/20101023',
      'com.paulscode/codecwav/20101023', 'com.paulscode/libraryjavasound/20101123', 'com.paulscode/librarylwjglopenal/20100824',
      'com.paulscode/soundsystem/20120107', 'com.mojang/netty/1.8.8', 'com.mojang/realms/1.3.5',
      'org.apache.commons/commons-compress/1.8.1', 'org.apache.httpcomponents/httpclient/4.3.3', 'commons-logging/commons-logging/1.1.3',
      'org.apache.httpcomponents/httpcore/4.3.2', 'java3d/vecmath/1.3.1', 'net.sf.trove4j/trove4j/3.0.3',
      'com.ibm.icu/icu4j-core-mojang/51.2', 'io.netty/netty-all/4.0.10.Final', 'commons-io/commons-io/2.4',
      'commons-codec/commons-codec/1.9', 'net.java.jinput/jinput/2.0.5', 'net.java.jutils/jutils/1.0.0',
      'com.google.code.gson/gson/2.2.4', 'org.apache.logging.log4j/log4j-api/2.0-beta9-fixed',
      'org.apache.logging.log4j/log4j-core/2.0-beta9-fixed', 'tv.twitch/twitch/5.16']
# Forge universal FIRST, the copy downloaded on its own (DIR/dl/forge-universal.jar): the server install's copy sits next
# to minecraft_server.1.7.10.jar, which its manifest Class-Path then puts before the client ("binary discrepency" on every
# Forge patch, docs/production_test.md trap 1; it happened again on 22 Oktober 2026 when this tool took the server's copy)
UNIVERSAL = os.path.join(P, 'dl', 'forge-universal.jar')
if not os.path.isfile(UNIVERSAL):
    sys.exit('need %s: the Forge 1.7.10-10.13.4.1614 universal jar downloaded on its own, not the server install\'s copy'
             % UNIVERSAL)
jars = [UNIVERSAL]
for root, d, fs in os.walk(os.path.join(SRV, 'libraries')):          # Forge's own libraries (installer)
    for f in fs:
        if f.endswith('.jar') and 'minecraft_server' not in f and 'forge' not in f:
            jars.append(os.path.join(root, f))
for coord in mc:
    hits = [p for p in glob.glob(os.path.join(G, coord, '*', '*.jar')) if 'sources' not in p and 'natives' not in p and 'javadoc' not in p]
    if not hits:
        print('MISSING', coord, file=sys.stderr)
    jars += hits[:1]
for nat in glob.glob(os.path.join(G, 'org.lwjgl.lwjgl/lwjgl-platform-natives-osx-arm64/*/*/*.jar')) + \
           glob.glob(os.path.join(G, 'net.java.jinput/jinput-platform-natives-osx-arm64/*/*/*.jar')):
    z = zipfile.ZipFile(nat)
    for n in z.namelist():
        if n.endswith(('.dylib', '.jnilib')):
            open(os.path.join(P, 'natives', os.path.basename(n)), 'wb').write(z.read(n))
jars.append(os.path.expanduser('~/.gradle/caches/retro_futura_gradle/mc-vanilla/1.7.10/client.jar'))
open(os.path.join(P, 'classpath.txt'), 'w').write(':'.join(jars))
print(len(jars), 'jars;', 'natives:', os.listdir(os.path.join(P, 'natives')))
