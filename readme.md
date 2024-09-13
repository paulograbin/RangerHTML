mvn package

java -Xlog:gc -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC --class-path target/downloadHTML-1.0-SNAPSHOT.jar com.paulograbin.Main
java -Xlog:gc -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC --class-path target/downloadHTML-1.0-SNAPSHOT.jar com.paulograbin.Main


native-image --gc=epsilon -O3 -march=native --enable-preview -cp target/downloadHTML-1.0-SNAPSHOT.jar -o download_native_experiment com.paulograbin.Main