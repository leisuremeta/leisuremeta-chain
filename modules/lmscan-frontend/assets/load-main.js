if (process.env.NODE_ENV === 'production') {
  import('../target/scala-3.2.1/leisuremeta-chain-lmscan-frontend-opt/main.js').then(
    (LmScan) => LmScan.launchApp(process.env.BACKEND_URL, process.env.BACKEND_PORT)
  );
} else {
  import('../target/scala-3.2.1/leisuremeta-chain-lmscan-frontend-fastopt/main.js').then(
    (LmScan) => LmScan.launchApp(process.env.BACKEND_URL, process.env.BACKEND_PORT)
  );
}
