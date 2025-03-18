rm -rf target

mvn clean package

native-image --gc=epsilon -O3 -march=native --enable-preview -cp target/downloadHTML-1.0-SNAPSHOT.jar -o download_native_experiment com.paulograbin.Main

sudo mv download_native_experiment /opt/htmlDownloader