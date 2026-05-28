import type { RefObject } from "react";
import { APPOINTMENT_TEXT } from "@/lib/appointmentText";
import type { TransactionType } from "@/lib/transactionType";
import type { ChatRoomDetail } from "./floatingTypes";
import { formatListingLabel } from "./floatingUtils";

type ChatFloatingProductPanelProps = {
  room: ChatRoomDetail;
  listingImageUrl: string | null;
  listingTransactionType: TransactionType;
  showAppointmentMenu: boolean;
  appointmentMenuRef: RefObject<HTMLDivElement | null>;
  onToggleAppointmentMenu: () => void;
  onOpenAppointment: () => void;
  onLocationCheck: () => void;
};

export function ChatFloatingProductPanel({
  room,
  listingImageUrl,
  listingTransactionType,
  showAppointmentMenu,
  appointmentMenuRef,
  onToggleAppointmentMenu,
  onOpenAppointment,
  onLocationCheck
}: ChatFloatingProductPanelProps) {
  return (
    <section className="chat-float-product">
      <div className="chat-float-product-media">
        {listingImageUrl ? <img src={listingImageUrl} alt={room.listing_title} /> : <div className="chat-float-product-placeholder" />}
      </div>
      <div className="chat-float-product-copy">
        <strong>{room.listing_title}</strong>
        <p>{formatListingLabel(room.listing_price, listingTransactionType)}</p>
      </div>
      <div className="chat-float-action-wrap" ref={appointmentMenuRef}>
        <button type="button" className="chat-float-action-toggle" onClick={onToggleAppointmentMenu}>
          <span>{APPOINTMENT_TEXT.quick.label}</span>
          <span aria-hidden="true">{showAppointmentMenu ? "▴" : "▾"}</span>
        </button>
        {showAppointmentMenu ? (
          <div className="chat-float-action-menu">
            <button type="button" onClick={onOpenAppointment}>
              {APPOINTMENT_TEXT.quick.openMenu}
            </button>
            <button type="button" onClick={onLocationCheck}>
              {APPOINTMENT_TEXT.quick.placePrefix} {APPOINTMENT_TEXT.quick.placeSuffix}
            </button>
          </div>
        ) : null}
      </div>
    </section>
  );
}
