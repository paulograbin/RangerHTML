mvn package

java -Xlog:gc -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC --class-path target/downloadHTML-1.0-SNAPSHOT.jar com.paulograbin.Main
java -Xlog:gc -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC --class-path target/downloadHTML-1.0-SNAPSHOT.jar com.paulograbin.Main


native-image --gc=epsilon -O3 -march=native --enable-preview -cp target/downloadHTML-1.0-SNAPSHOT.jar -o download_native_experiment com.paulograbin.Main


# How to add it to systemctl

- Copy script and binary to /opt/htmlDownloader
- Copy timer and service description to /etc/systemd/system
- Inside the directory above run: sudo systemctl enable myservice.service
- Inside the directory above run: sudo systemctl enable myservice.timer