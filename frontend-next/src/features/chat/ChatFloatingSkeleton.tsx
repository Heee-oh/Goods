export function ChatFloatingSkeleton() {
  return (
    <>
      <section className="chat-float-product chat-float-product-skeleton" aria-hidden="true">
        <div className="chat-float-product-media chat-float-skeleton-box" />
        <div className="chat-float-product-copy">
          <div className="chat-float-skeleton-line chat-float-skeleton-line-title" />
          <div className="chat-float-skeleton-line chat-float-skeleton-line-price" />
        </div>
        <div className="chat-float-skeleton-pill" />
      </section>
      <div className="chat-float-messages chat-float-messages-skeleton" aria-hidden="true">
        <div className="chat-float-message-skeleton mine" />
        <div className="chat-float-message-skeleton" />
        <div className="chat-float-message-skeleton mine short" />
      </div>
    </>
  );
}
