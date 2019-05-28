package com.wirecard.wpp.integration.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigProperties {

    @Bean
    @ConfigurationProperties(prefix = "ee.cc")
    public WirecardConfig ccConfig() {
        return new WirecardConfig();
    }
    @Bean
    @ConfigurationProperties(prefix = "ee.eps")
    public WirecardConfig epsConfig() {
        return new WirecardConfig();
    }

    public class WirecardConfig {
        private String maid;
        private String username;
        private String password;
        private String secretkey;

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getMaid() {
            return maid;
        }

        public String getSecretkey() {
            return secretkey;
        }

        public void setMaid(String maid) {
            this.maid = maid;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public void setSecretkey(String secretkey) {
            this.secretkey = secretkey;
        }
    }
}
