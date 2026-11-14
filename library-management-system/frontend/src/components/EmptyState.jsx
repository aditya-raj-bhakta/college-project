import Icon from './Icon';

export default function EmptyState({ title, icon = 'library', children }) {
  return (
    <div className="empty-state">
      <div className="empty-icon"><Icon name={icon} size={18} /></div>
      <h3>{title}</h3>
      {children && <p>{children}</p>}
    </div>
  );
}
