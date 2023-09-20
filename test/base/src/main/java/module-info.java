module test.flow.base {
    requires transitive org.assertj.core;
    requires transitive org.junit.jupiter.api;
    //noinspection requires-transitive-automatic
    requires transitive org.mockito;
    //noinspection requires-transitive-automatic
    requires transitive org.mockito.junit.jupiter;

    requires transitive net.bytebuddy;
    requires transitive net.bytebuddy.agent;
    requires transitive org.junit.jupiter.engine;
}
