FROM ubuntu:20.04

RUN useradd -ms /bin/bash bot
USER bot

RUN mkdir -p /home/bot/app
WORKDIR /home/bot/app

USER root

ENV DEBIAN_FRONTEND noninteractive
ENV USE_SANDBOX false

RUN apt-get update -y && apt-get install -y openjdk-11-jdk build-essential curl wget git gdebi p7zip-rar locales rar zip unzip

RUN curl -sL https://deb.nodesource.com/setup_12.x | bash -
RUN apt-get install -y nodejs

RUN wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
RUN gdebi --n google-chrome-stable_current_amd64.deb
RUN rm google-chrome-stable_current_amd64.deb

# Locale
ENV LC_ALL C.UTF-8

RUN wget https://gif.ski/gifski-0.10.1.zip
RUN unzip -j gifski-0.10.1.zip -d gifski
RUN gdebi --n gifski/gifski_0.10.1_amd64.deb
RUN rm -rf gifski
RUN rm gifski-0.10.1.zip

USER bot

RUN git clone https://github.com/saidgadjiev/tgs-to-gif.git
RUN cd tgs-to-gif && npm install

RUN wget https://github.com/jankovicsandras/imagetracerjava/raw/master/ImageTracer.jar

USER root

RUN sed -Ei 's/^# deb-src /deb-src /' /etc/apt/sources.list
RUN apt-get update -y && apt-get -y build-dep imagemagick && \
apt-get install -y libwebp-dev libopenjp2-7-dev librsvg2-dev libde265-dev libheif-dev

RUN echo 'Imagemagick 7.0.10-19'
RUN cd /usr/src/ && \
wget https://www.imagemagick.org/download/ImageMagick.tar.bz2 && \
tar xvf ImageMagick.tar.bz2 && cd ImageMagick* && \
./configure && make -j 4 && \
make install && \
make distclean && ldconfig

RUN apt-get update -y
RUN apt-get install -y libopus-dev libmp3lame-dev libfdk-aac-dev libvpx-dev libx264-dev yasm libass-dev libtheora-dev libvorbis-dev libopencore-amrnb-dev libopencore-amrwb-dev mercurial cmake
RUN cd /usr/src && \
wget https://github.com/videolan/x265/archive/master.zip | unzip && \
unzip master.zip && \
rm /usr/src/master.zip && \
cd x265-master/build/linux && \
cmake -G "Unix Makefiles" -DCMAKE_INSTALL_PREFIX="/home/bot/ffmpeg_build" -DENABLE_SHARED:bool=off ../../source && \
make && make install

ENV PATH="/home/bot/ffmpeg_build/bin/:${PATH}"

RUN rm /usr/src/ImageMagick.tar.bz2

RUN mkdir -p /usr/src/libaom && \
  cd /usr/src/libaom && \
  git clone https://aomedia.googlesource.com/aom && \
  cmake ./aom && \
  make && make install

RUN apt-get install -y libspeex-dev

RUN apt-get install -y libgmp3-dev libbluray-dev libmfx-dev libshine-dev libsnappy-dev libsoxr-dev libtwolame-dev \
libvidstab-dev libxml2-dev

RUN cd /usr/src && \
wget -O- http://ffmpeg.org/releases/ffmpeg-snapshot.tar.bz2 | tar xj && \
cd ffmpeg && \
PKG_CONFIG_PATH="/home/bot/ffmpeg_build/lib/pkgconfig" \
   ./configure \
  --prefix="/home/bot/ffmpeg_build" \
  --pkg-config-flags="--static" \
  --extra-cflags="-I/home/bot/ffmpeg_build/include" \
  --extra-ldflags="-L/home/bot/ffmpeg_build/lib" \
  --bindir="/home/bot/ffmpeg" \
  --disable-ffplay \
  --enable-encoders \
  --enable-muxers \
  --enable-demuxers \
  --enable-filters \
  --disable-devices \
  --enable-decoders && \
make -j 4 && make install

ENV PATH="/home/bot/ffmpeg/:${PATH}"

RUN echo 'calibre-5'
RUN wget -nv -O- https://download.calibre-ebook.com/linux-installer.sh | sh /dev/stdin install_dir=/home/bot isolated=y
ENV PATH="/home/bot/calibre/:${PATH}"

RUN apt-get update -y
RUN apt-get install -y img2pdf
RUN apt-get install -y djvulibre-bin
RUN apt-get install -y libxss1
RUN apt-get install -y timidity
RUN apt-get install -y poppler-utils
RUN apt-get install -y python

RUN apt-get update -y
RUN apt-get install -y sox
RUN apt-get install -y libsox-fmt-all

RUN apt-get clean -y && apt-get autoclean -y && apt-get autoremove -y && rm -rf /var/lib/apt/lists/* /var/tmp/*

USER bot

COPY ./license/license-19.lic ./license/
COPY ./target/app.jar .
COPY ./scripts/jpegtoepub.py .


EXPOSE 8080
#ENTRYPOINT ["java", "-Xss10m", "-Xmx8g", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/home/bot/app/oom", "-jar", "app.jar"]
#ENTRYPOINT ["java", "-Xss10m", "-Xmx8g", "-XX:+HeapDumpOnOutOfMemoryError", \
#"-XX:HeapDumpPath=/home/bot/app/oom", \
#"-Dcom.sun.management.jmxremote", \
#"-Dcom.sun.management.jmxremote.authenticate=false", \
#"-Dcom.sun.management.jmxremote.ssl=false", \
#"-Dcom.sun.management.jmxremote.port=9010", \
#"-Dcom.sun.management.jmxremote.rmi.port=9010", \
#"-Djava.rmi.server.hostname=192.168.99.100", \
#"-jar", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "app.jar"]

ENTRYPOINT ["java", "-Xss10m", "-Xmx8g", "-XX:+HeapDumpOnOutOfMemoryError", "-jar", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "app.jar"]
