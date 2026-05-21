import { getTransactionLabel, type TransactionType } from "./transactionType";

export type ListingStatus = "DRAFT" | "PUBLISHED" | "RESERVED" | "SOLD_OUT" | string;

export function getListingStatusLabel(status: ListingStatus, transactionType: TransactionType | null | undefined) {
  switch (status) {
    case "RESERVED":
      return "\uC608\uC57D\uC911";
    case "SOLD_OUT":
      return `${getTransactionLabel(transactionType)}\uC644\uB8CC`;
    case "PUBLISHED":
    default:
      return `${getTransactionLabel(transactionType)}\uC911`;
  }
}

export function shouldShowStatusBadge(status: ListingStatus) {
  return status === "RESERVED" || status === "SOLD_OUT";
}

export function getListingStatusTone(status: ListingStatus) {
  if (status === "RESERVED") {
    return "reserved";
  }

  if (status === "SOLD_OUT") {
    return "completed";
  }

  return "default";
}
