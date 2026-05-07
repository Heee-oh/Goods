import type { CapacitorConfig } from "@capacitor/cli";

const config: CapacitorConfig = {
  appId: "com.daangn.market",
  appName: "Daangn",
  webDir: "dist",
  server: {
    androidScheme: "https"
  }
};

export default config;
