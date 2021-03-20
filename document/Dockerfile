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

RUN rm /usr/src/ImageMagick.tar.bz2

RUN echo 'calibre-5'
RUN wget -nv -O- https://download.calibre-ebook.com/linux-installer.sh | sh /dev/stdin install_dir=/home/bot isolated=y
ENV PATH="/home/bot/calibre/:${PATH}"

RUN apt-get install -y img2pdf
RUN apt-get install -y djvulibre-bin
RUN apt-get install -y libxss1
RUN apt-get install -y timidity
RUN apt-get install -y poppler-utils
RUN apt-get update -y && apt-get install -y python

RUN apt-get clean -y && apt-get autoclean -y && apt-get autoremove -y && rm -rf /var/lib/apt/lists/* /var/tmp/*

USER bot

COPY ./license/license-19.lic ./license/
COPY ./target/app.jar .
COPY ./scripts/jpegtoepub.py .


EXPOSE 8080
ENTRYPOINT ["java", "-Xss10m", "-Xmx8g", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/home/bot/app/oom", "-jar", "app.jar"]
#ENTRYPOINT ["java", "-Xss10m", "-Xmx8g", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/home/bot/app/oom", "-jar", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "app.jar"]
