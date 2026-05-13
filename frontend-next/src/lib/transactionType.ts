export type TransactionType = "sell" | "trade" | "free";

export function isFreeTransaction(transactionType: TransactionType | null | undefined) {
  return transactionType === "free";
}

export function getTransactionLabel(transactionType: TransactionType | null | undefined) {
  switch (transactionType) {
    case "trade":
      return "\uAD50\uD658";
    case "free":
      return "\uB098\uB214";
    case "sell":
    default:
      return "\uD310\uB9E4";
  }
}
