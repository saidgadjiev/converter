FROM ubuntu:20.04

RUN useradd -ms /bin/bash bot
USER bot

USER root

ENV DEBIAN_FRONTEND noninteractive
ENV USE_SANDBOX false

RUN apt-get update -y
RUN apt-get upgrade -y
RUN apt-get install -y make git zlib1g-dev libssl-dev gperf cmake clang-10 libc++-dev libc++abi-dev

RUN git clone --recursive https://github.com/tdlib/telegram-bot-api.git
RUN cd telegram-bot-api && rm -rf build && mkdir build && cd build && \
export CXXFLAGS="-stdlib=libc++" && \
CC=/usr/bin/clang-10 CXX=/usr/bin/clang++-10 cmake -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX:PATH=/usr/local .. && \
cmake --build . --target install

USER bot
RUN mkdir -p /home/bot/workdir
RUN mkdir -p /home/bot/logs
ENTRYPOINT ["telegram-bot-api", "--http-stat-port=8082", "--dir=/home/bot/workdir", "--local"]
