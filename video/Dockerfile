FROM ubuntu:20.04

RUN useradd -ms /bin/bash bot
USER bot

RUN mkdir -p /home/bot/app
WORKDIR /home/bot/app

USER root

ENV DEBIAN_FRONTEND noninteractive
ENV USE_SANDBOX false

RUN apt-get update -y && apt-get install -y openjdk-11-jdk build-essential curl wget git gdebi p7zip-rar locales rar zip unzip

# Locale
ENV LC_ALL C.UTF-8

RUN sed -Ei 's/^# deb-src /deb-src /' /etc/apt/sources.list
RUN apt-get update -y && apt-get -y build-dep imagemagick && \
apt-get install -y librsvg2-dev libopenjp2-7-dev libwebp-dev libopus-dev libmp3lame-dev libfdk-aac-dev libvpx-dev libx264-dev yasm libass-dev \
libtheora-dev libvorbis-dev libopencore-amrnb-dev libopencore-amrwb-dev mercurial cmake
RUN cd /usr/src && \
wget https://github.com/videolan/x265/archive/master.zip | unzip && \
unzip master.zip && \
rm /usr/src/master.zip && \
cd x265-master/build/linux && \
cmake -G "Unix Makefiles" -DCMAKE_INSTALL_PREFIX="/home/bot/ffmpeg_build" -DENABLE_SHARED:bool=off ../../source && \
make && make install

ENV PATH="/home/bot/ffmpeg_build/bin/:${PATH}"

RUN mkdir -p /usr/src/libaom && \
  cd /usr/src/libaom && \
  git clone https://aomedia.googlesource.com/aom && \
  cmake ./aom && \
  make && make install

RUN apt-get install -y libspeex-dev libgmp3-dev libbluray-dev libmfx-dev libshine-dev libsnappy-dev libsoxr-dev libtwolame-dev \
libvidstab-dev libxml2-dev

RUN cd /usr/src && \
wget -O- http://ffmpeg.org/releases/ffmpeg-4.4.tar.bz2 | tar xj && \
cd ffmpeg-4.4 && \
PKG_CONFIG_PATH="/home/bot/ffmpeg_build/lib/pkgconfig" \
   ./configure \
  --prefix="/home/bot/ffmpeg_build" \
  --pkg-config-flags="--static" \
  --extra-cflags="-I/home/bot/ffmpeg_build/include" \
  --extra-ldflags="-L/home/bot/ffmpeg_build/lib" \
  --extra-libs="-lpthread -lm" \
  --bindir="/home/bot/ffmpeg" \
  --enable-pthreads \
  --enable-libopencore-amrnb \
  --enable-libopencore-amrwb \
  --enable-libspeex \
  --enable-version3 \
  --enable-gpl \
  --enable-libass \
  --enable-libbluray \
  --enable-libfdk-aac \
  --enable-libfreetype \
  --enable-libmp3lame \
  --enable-libopenjpeg \
  --enable-libopus \
  --enable-libtheora \
  --enable-libshine \
  --enable-libsnappy \
  --enable-libsoxr \
  --enable-libvorbis \
  --enable-libwebp \
  --enable-libxml2 \
  --enable-lzma \
  --enable-zlib \
  --enable-gmp \
  --enable-libmfx \
  --enable-libvidstab \
  --enable-libtwolame \
  --enable-libvpx \
  --enable-libx264 \
  --enable-fontconfig \
  --enable-iconv \
  --enable-bzlib \
  --enable-libaom \
  --enable-libx265 \
  --enable-nonfree && \
make -j 4 && make install

ENV PATH="/home/bot/ffmpeg/:${PATH}"

RUN apt-get install -y timidity

RUN curl -sL https://deb.nodesource.com/setup_12.x | bash -
RUN apt-get install -y nodejs

RUN wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
RUN gdebi --n google-chrome-stable_current_amd64.deb
RUN rm google-chrome-stable_current_amd64.deb

RUN wget https://gif.ski/gifski-0.10.1.zip
RUN unzip -j gifski-0.10.1.zip -d gifski
RUN gdebi --n gifski/gifski_0.10.1_amd64.deb
RUN rm -rf gifski
RUN rm gifski-0.10.1.zip

USER bot

RUN git clone https://github.com/saidgadjiev/tgs-to-gif.git
RUN cd tgs-to-gif && npm install

USER root
RUN apt-get update -y
RUN apt-get install -y libxss1

#Image magick
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

RUN apt-get update -y && apt-get install -y mediainfo

RUN mkdir -p /usr/src/mediainfo && \
  cd /usr/src/mediainfo && \
  wget https://mediaarea.net/download/binary/libzen0/0.4.39/libzen0v5_0.4.39-1_amd64.xUbuntu_20.04.deb && \
  wget https://mediaarea.net/download/binary/libmediainfo0/21.09/libmediainfo0v5_21.09-1_amd64.xUbuntu_20.04.deb && \
  wget https://mediaarea.net/download/binary/mediainfo/21.09/mediainfo_21.09-1_amd64.xUbuntu_20.04.deb && \
  dpkg -i *deb

RUN rm /usr/src/ImageMagick.tar.bz2

RUN apt-get clean -y && apt-get autoclean -y && apt-get autoremove -y && rm -rf /var/lib/apt/lists/* /var/tmp/*

USER bot

COPY ./target/app.jar .


EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
#ENTRYPOINT ["java", "-jar", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "app.jar"]
