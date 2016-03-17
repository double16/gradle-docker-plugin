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

    @Input
    @Optional
    Date since

    Class loadClass(String className) {
        Thread.currentThread().contextClassLoader.loadClass(className)
    }

    private createLoggingCallback() {
        Class callbackClass = loadClass("com.github.dockerjava.core.command.LogContainerResultCallback")
        def delegate = callbackClass.newInstance()

        Class enhancerClass = loadClass('net.sf.cglib.proxy.Enhancer')
        def enhancer = enhancerClass.getConstructor().newInstance()
        enhancer.setSuperclass(callbackClass)
        enhancer.setCallback([

            invoke: {Object proxy, java.lang.reflect.Method method, Object[] args ->
                if ("onNext" == method.name) {
                  def frame = args[0]
                  switch (frame.streamType as String) {
                    case "STDOUT":
                    case "RAW":
                        System.out.print(new String(frame.payload))
                        break
                    case "STDERR":
                        System.err.print(new String(frame.payload))
                        break
                  }
                }
                method.invoke(delegate, args)
            }

        ].asType(loadClass('net.sf.cglib.proxy.InvocationHandler')))

        enhancer.create()
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

        if (since != null) {
            logsCommand.withSince((int) (since.time / 1000))
        }
    }
}

