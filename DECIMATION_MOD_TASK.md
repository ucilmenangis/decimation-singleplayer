# Task: Fix Singleplayer Loot Tables in Decimation Mod (Minecraft 1.7.10)

## Konteks

Mod **Decimation** (Minecraft Forge 1.7.10, zombie apocalypse survival mod) sudah tidak
di-update dan situs resminya (`decimation.net` / `mcdecimation.net`) sudah mati total —
server maupun webnya sudah dimatikan permanen oleh developernya. Ini bukan mod hasil
bajakan/pihak lain yang aktif dijual; ini konten gratis yang official-nya sudah
ditinggalkan (abandonware), dan file ini didapat oleh user secara legal lewat modpack
Technic ("Decimation 1.7.10" oleh uploader `linusrhone`) yang sudah dites jalan
di Minecraft 1.7.10 miliknya sendiri.

File jar yang jadi objek kerja: `Decimation.jar`, ukuran **238.2 MB** (kemungkinan besar
karena banyak custom asset — model senjata, texture, suara — dibundle langsung di
dalam jar, bukan cuma kode).

**PENTING — batasan kerja:**
- Tujuan HANYA untuk penggunaan pribadi user di instalasi Minecraft-nya sendiri.
- JANGAN redistribusi/republish hasil modifikasi ke publik — ini murni personal fix,
  bukan proyek buat dipublikasikan ulang.
- SELALU backup jar original sebelum dipatch, jangan overwrite langsung.

## Masalah yang mau diselesaikan

Dulu waktu mod ini masih aktif di-hosting di server publik, ada fitur config berbasis
YAML untuk atur loot table (isi crate, mobil, dan objek lootable lain).

**Gejala saat ini:**
- Di **multiplayer** (waktu masih ada server): crate, car (mobil), dan objek lootable
  lain bisa di-loot normal.
- Di **singleplayer** (integrated server): benda-benda yang sama (crate, car, dll)
  **TIDAK BISA** di-loot / loot table-nya tidak berfungsi.

User sudah cek langsung: mod jalan normal di singleplayer (senjata bisa ditembakkan,
mob bisa spawn), tapi loot dari crate/car spesifik yang bermasalah.

## Hipotesis penyebab (belum diverifikasi — perlu dicek langsung di kode)

Pattern umum di mod Forge 1.7.10 sejenis yang punya "official server" dan sistem loot
server-authoritative:

1. **Gate eksplisit** semacam `isDedicatedServer()` / `!world.isRemote` /
   `!Minecraft.getMinecraft().isSingleplayer()` yang sengaja nge-lock logic loot supaya
   cuma jalan di dedicated server.
2. **Path loading config YAML** loot table dibaca dengan cara yang cuma valid/terdeteksi
   di context dedicated server, sehingga integrated server (yang jalan pas main
   singleplayer) skip loading config itu sama sekali.
3. **Event handler loot cuma register ke event lifecycle dedicated server**
   (misal `FMLDedicatedServerStartingEvent`), bukan event umum yang juga fire di
   integrated server.

Ketiganya sifatnya **gate/kondisi di kode**, bukan file yang hilang — jadi solusinya
adalah decompile → temukan kondisi ini → patch → recompile & repack.

## Langkah kerja yang diminta

1. **Cari file jar Decimation** di direktori instance Prism Launcher (folder
   `.minecraft/mods/` di instance "Decimation 1.7.10"). Kalau belum ada di working
   directory, minta user tunjukkan lokasi persisnya.
2. **Backup** file jar original sebelum diapa-apain.
3. **Decompile** jar menggunakan Vineflower atau CFR (unzip jar dulu buat lihat isi
   struktur — jar itu format zip, bisa langsung diekstrak untuk melihat resource/asset
   vs `.class` file Java).
4. **Cari** dengan grep/search keyword berikut di source hasil decompile:
   `isDedicatedServer`, `isSingleplayer`, `isRemote`, `loot`, `Loot`, `crate`, `Crate`,
   `car`, `Car`, `yaml`, `yml`, `LootTable`, `FMLDedicatedServerStartingEvent`.
5. **Identifikasi** class/method mana yang jadi gate penyebab loot table tidak jalan
   di integrated server (singleplayer).
6. **Laporkan temuan ke user dulu** sebelum patch — jelaskan persis baris/kondisi mana
   yang jadi penyebab, dan opsi fix-nya (misal: ubah kondisi `if` supaya juga true di
   integrated server, atau samakan path loading config untuk kedua context).
7. Setelah user setuju, **patch bytecode/source**, **recompile**, dan **repack ulang
   jar** (perlu setup JDK yang sesuai — mod era 1.7.10 Forge umumnya pakai Java 7/8;
   cek `MANIFEST.MF` di jar buat konfirmasi target version kalau ada infonya).
8. **Test** hasil jar yang sudah dipatch di instance singleplayer user.

## Definition of done

- User bisa loot crate, car, dan objek lootable lain di singleplayer, sama seperti
  behavior di multiplayer dulu.
- Jar hasil patch tidak merusak fitur lain yang sudah jalan (senjata, mob spawn, dll).
- Ada file jar original yang di-backup, terpisah dari jar hasil patch.
