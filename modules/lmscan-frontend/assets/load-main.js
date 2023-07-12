if (process.env.NODE_ENV === "production") {
  import(
    "../target/scala-3.3.0-RC4/leisuremeta-chain-lmscan-frontend-fastopt/main.js"
  ).then((LmScan) => LmScan.launchApp(process.env.BASE_API_URL));
} else {
  import(
    "../target/scala-3.3.0-RC4/leisuremeta-chain-lmscan-frontend-fastopt/main.js"
  ).then((LmScan) => LmScan.launchApp(process.env.BASE_API_URL));
}
