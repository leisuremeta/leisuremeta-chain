if (process.env.NODE_ENV === "production") {
  import(
    "../target/scala-3.2.1/leisuremeta-chain-lmscan-frontend-opt/main.js"
  ).then((LmScan) => LmScan.launchApp(process.env.BASE_API_URL));
} else {
  import(
    "../target/scala-3.2.1/leisuremeta-chain-lmscan-frontend-fastopt/main.js"
  ).then((LmScan) => LmScan.launchApp(process.env.BASE_API_URL));
}
