mvn clean package

native-image --gc=epsilon -O3 -march=native -cp target/downloadHTML-1.0-SNAPSHOT.jar -o download_native_experiment com.paulograbin.Main

sudo cp download_native_experiment /opt/htmlDownloader