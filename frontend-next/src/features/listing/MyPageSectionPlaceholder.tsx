type MyPageSectionPlaceholderProps = {
  badge: string;
  title: string;
  summary: string;
  emptyMessage: string;
};

export function MyPageSectionPlaceholder({
  badge,
  title,
  summary,
  emptyMessage
}: MyPageSectionPlaceholderProps) {
  return (
    <section className="profile-inline-shell">
      <div className="profile-page-card">
        <div className="profile-page-avatar" aria-hidden="true">
          {badge}
        </div>
        <div className="profile-page-copy">
          <strong>{title}</strong>
          <span>{summary}</span>
        </div>
      </div>
      <div className="profile-page-empty">{emptyMessage}</div>
    </section>
  );
}
