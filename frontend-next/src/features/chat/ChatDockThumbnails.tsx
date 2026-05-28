export function ChatThumbnail({
  profileImage,
  partnerNickname
}: {
  profileImage: string | null;
  partnerNickname: string;
}) {
  if (profileImage) {
    return <img className="chat-list-thumb chat-list-thumb-profile" src={profileImage} alt={partnerNickname} />;
  }

  return (
    <div className="chat-list-thumb chat-list-thumb-empty">
      <div className="chat-list-thumb-mark" />
    </div>
  );
}

export function ListingThumbnail({
  imageUrl,
  listingTitle
}: {
  imageUrl: string | null;
  listingTitle: string | null;
}) {
  if (imageUrl) {
    return <img className="chat-list-thumb chat-list-thumb-listing" src={imageUrl} alt={listingTitle ?? ""} />;
  }

  return (
    <div className="chat-list-thumb chat-list-thumb-listing chat-list-thumb-empty">
      <div className="chat-list-thumb-mark" />
    </div>
  );
}
