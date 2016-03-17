package com.bmuschko.gradle.docker.tasks.container

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

class DockerLogsContainer extends DockerExistingContainer {
    @Input
    @Optional
    Boolean follow

    @Input
    @Optional
    def tail

    @Input
    @Optional
    Boolean showTimestamps

    private createLoggingCallback() {
      def callbackClass = Thread.currentThread().contextClassLoader.loadClass("com.github.dockerjava.core.command.LogContainerResultCallback")
      System.err.println "callback class is ${callbackClass.name}"
      def callback = [onNext: { frame ->
          switch (frame.streamType as String) {
            case "STDOUT":
            case "RAW":
                System.out.print(frame.payload)
                break
            case "STDERR":
                System.err.print(frame.payload)
                break
          }
      }].asType(callbackClass)
      System.err.println "${callback.class.name} -> ${callback.class.superclass.name}"
    }

    @Override
    void runRemoteCommand(dockerClient) {
        logger.info "Logs for container with ID '${getContainerId()}'."
        def logCommand = dockerClient.logContainerCmd(getContainerId())
        setContainerCommandConfig(logCommand)
        logCommand.exec(createLoggingCallback())?.awaitCompletion()
    }

    private void setContainerCommandConfig(logsCommand) {
        if (follow != null) {
            logger.info "Following stream = ${follow}"
            logsCommand.withFollowStream(follow)
        }

        if (showTimestamps != null) {
            logsCommand.withTimestamps(showTimestamps)
        }

        logsCommand.withStdOut(true).withStdErr(true)

        if (tail instanceof Boolean) {
            if (tail) {
                logger.info "Tailing all"
                logsCommand.withTailAll()
            }
        } else if (tail != null) {
            def count = tail as Integer
            logger.info "Tailing ${count} lines"
            logsCommand.withTail(count)
        }

        //TODO: logsCommand.withSince(Integer since)
    }
}

