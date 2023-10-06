package io.quarkiverse.dnsjava.deployment;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.jboss.logging.Logger;
import org.xbill.DNS.Header;
import org.xbill.DNS.ResolverConfig;
import org.xbill.DNS.SimpleResolver;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import javassist.*;

class DnsjavaProcessor {

    private static final String FEATURE = "dnsjava";

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void runtimeInitializeClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitialized,
            BuildProducer<RuntimeReinitializedClassBuildItem> runtimeReinitialized) {

        final Set<String> initializeAtRuntime = new HashSet<>();
        initializeAtRuntime.add(Header.class.getName());
        initializeAtRuntime.add("org.xbill.DNS.NioUdpClient");

        for (final String toInitAtRuntime : initializeAtRuntime) {
            runtimeInitialized.produce(new RuntimeInitializedClassBuildItem(toInitAtRuntime));
        }
    }

    @BuildStep
    SystemPropertyBuildItem systemPropertyBuildItem() {
        return new SystemPropertyBuildItem("dnsjava.configprovider.skipinit", "true");
    }
}
