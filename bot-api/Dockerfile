FROM ubuntu:20.04

RUN useradd -ms /bin/bash bot
USER bot

USER root

ENV DEBIAN_FRONTEND noninteractive
ENV USE_SANDBOX false

RUN apt-get update -y
RUN apt-get upgrade -y
RUN apt-get -y install make git zlib1g-dev libssl-dev gperf cmake g++
RUN git clone --recursive https://github.com/tdlib/telegram-bot-api.git
RUN cd telegram-bot-api && \
rm -rf build && \
mkdir build && \
cd build && \
export CXXFLAGS="" && \
cmake -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX:PATH=/usr/local .. && \
cmake --build . --target install