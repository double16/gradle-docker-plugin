package com.bmuschko.gradle.docker.tasks.container

import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.StreamType
import com.github.dockerjava.core.command.LogContainerResultCallback
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

class StandardOutErrLogCallback extends LogContainerResultCallback {
    @Override
    void onNext(Frame item) {
        switch (item.streamType) {
            case StreamType.STDOUT:
            case StreamType.RAW:
                System.out.println(item.payload)
                break
            case StreamType.STDERR:
                System.err.println(item.payload)
                break
        }
    }
}

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

    @Override
    void runRemoteCommand(dockerClient) {
        logger.quiet "Logs for container with ID '${getContainerId()}'."
        def logCommand = dockerClient.logContainerCmd(getContainerId())
        setContainerCommandConfig(logCommand)
        logCommand.exec(new StandardOutErrLogCallback())?.awaitCompletion()
    }

    private void setContainerCommandConfig(logsCommand) {
        if (follow != null) {
            logger.info "Following stream = ${follow}"
            logsCommand.withFollowStream(follow);
        }

        if (showTimestamps != null) {
            logsCommand.withTimestamps(showTimestamps);
        }

        logsCommand.withStdOut(true);

        logsCommand.withStdErr(true);

        if (tail instanceof Boolean) {
            if (tail) {
                logger.info "Tailing all"
                logsCommand.withTailAll();
            }
        } else if (tail != null) {
            def count = tail as Integer
            logger.info "Tailing ${count} lines"
            logsCommand.withTail(count);
        }

        //TODO: logsCommand.withSince(Integer since);
    }
}
