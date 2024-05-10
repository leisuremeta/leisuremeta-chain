(async () => {
  let m
  if (process.env.NODE_ENV === "production") {
    m = await import("../target/scala-3.4.1/leisuremeta-chain-lmscan-frontend-opt/main.js")
  } else {
    m = await import("../target/scala-3.4.1/leisuremeta-chain-lmscan-frontend-fastopt/main.js")
  }
  m.LmScan.launch("app-container")
})()
