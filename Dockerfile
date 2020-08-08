# The base image
FROM ubuntu:18.04

# Install basic dependencies
RUN apt-get update && apt-get install -y \
	git \
    build-essential \
    openjdk-8-jdk \
	maven \
	wget \
	unzip \
	vim

# Set Java version
#RUN update-java-alternatives --set /usr/lib/jvm/java-1.8.0-openjdk-amd64

# Install MoFuzz
WORKDIR /workspace
RUN git clone https://github.com/hub-se/MoFuzz.git
WORKDIR /workspace/MoFuzz/mofuzz
RUN mvn package
WORKDIR /workspace/MoFuzz
