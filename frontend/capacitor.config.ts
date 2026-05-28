import type { CapacitorConfig } from "@capacitor/cli";

const config: CapacitorConfig = {
  appId: "com.goods.market",
  appName: "Goods",
  webDir: "dist",
  server: {
    androidScheme: "https"
  }
};

export default config;
