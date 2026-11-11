package com.netfieldpro;

import com.netfieldpro.auth.AuthGatewayFactory;
import com.netfieldpro.auth.NoOpAuthGatewayImpl;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class NoAuthRule implements TestRule {

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                AuthGatewayFactory.setForTesting(new NoOpAuthGatewayImpl());
                try {
                    base.evaluate();
                } finally {
                    AuthGatewayFactory.resetForTesting();
                }
            }
        };
    }
}

