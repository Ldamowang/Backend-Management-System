export interface TotpSetupResponse {
  secretKey: string
  qrCodeUri: string
  backupCodes: string[]
}

export interface TotpStatusResponse {
  enabled: boolean
}
