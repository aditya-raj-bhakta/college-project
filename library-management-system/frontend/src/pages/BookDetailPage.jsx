import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { booksApi, copiesApi } from '../api/services';
import { getErrorMessage, getFieldErrors } from '../api/client';
import { useToast } from '../context/ToastContext';
import useAsync from '../hooks/useAsync';
import BookCover from '../components/BookCover';
import BookFormModal from '../components/BookFormModal';
import ConfirmDialog from '../components/ConfirmDialog';
import Modal from '../components/Modal';
import Spinner from '../components/Spinner';
import Icon from '../components/Icon';
import DueStamp from '../components/DueStamp';
import { COPY_STATUS_LABELS, CopyStatus, HoldStatus } from '../components/Badges';
import { formatDate, formatDateTime, ordinal, plural } from '../utils/format';

const MANUAL_STATUSES = ['AVAILABLE', 'DAMAGED', 'LOST', 'WITHDRAWN'];

export default function BookDetailPage() {
  const { id } = useParams();
  const toast = useToast();
  const navigate = useNavigate();
  const { data, error, reload } = useAsync(
    () => Promise.all([booksApi.get(id), booksApi.copies(id), booksApi.queue(id), booksApi.categories()])
      .then(([book, copies, queue, categories]) => ({ book, copies, queue, categories })),
    [id]
  );
  const [editing, setEditing] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleteBusy, setDeleteBusy] = useState(false);
  const [editingCopy, setEditingCopy] = useState(null);

  if (error) return <div className="alert alert-error">{error}</div>;
  if (!data) return <Spinner />;
  const { book, copies, queue, categories } = data;
  const a = book.availability;

  const handleDelete = async () => {
    setDeleteBusy(true);
    try {
      await booksApi.remove(book.id);
      toast(`"${book.title}" deleted`);
      navigate('/books');
    } catch (err) {
      toast(getErrorMessage(err, 'Could not delete this title'), 'error');
      setDeleting(false);
    } finally {
      setDeleteBusy(false);
    }
  };

  return (
    <>
      <Link to="/books" className="back-link"><Icon name="arrowLeft" size={14} />Catalogue</Link>
      <div className="page-header">
        <div>
          <h1>{book.title}</h1>
          <p className="page-meta">{book.author}{book.publishedYear ? ` · ${book.publishedYear}` : ''} · <span className="mono">{book.isbn}</span></p>
        </div>
        <div className="header-actions">
          <button className="btn btn-ghost" onClick={() => setDeleting(true)}>Delete</button>
          <button className="btn btn-secondary" onClick={() => setEditing(true)}><Icon name="edit" />Edit details</button>
        </div>
      </div>

      <div className="title-grid">
        <aside className="title-aside">
          <div className="title-cover"><BookCover title={book.title} author={book.author} /></div>
          <dl className="title-facts">
            <div><dt>On the shelf</dt><dd className="is-accent">{a.available}</dd></div>
            <div><dt>On loan</dt><dd>{a.onLoan}</dd></div>
            <div><dt>Hold shelf</dt><dd>{a.onHoldShelf}</dd></div>
            <div><dt>Waiting</dt><dd>{a.waitingHolds}</dd></div>
          </dl>
          {book.category && <p className="title-category">{book.category}</p>}
          {book.description && <p className="title-description">{book.description}</p>}
        </aside>

        <div>
          <section className="card">
            <div className="card-header">
              <h2>Copies</h2>
              <span className="card-meta">{plural(a.total, 'copy', 'copies')} in circulation</span>
            </div>
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr><th>Barcode</th><th>Shelf</th><th>Status</th><th>Where it is</th><th>Acquired</th><th /></tr>
                </thead>
                <tbody>
                  {copies.map((c) => (
                    <tr key={c.id}>
                      <td className="mono cell-strong">{c.barcode}</td>
                      <td className="cell-sub-inline">{c.shelfLocation || '—'}</td>
                      <td><CopyStatus status={c.status} /></td>
                      <td>
                        {c.status === 'ON_LOAN' && c.borrowerName && (
                          <span className="cell-pair">{c.borrowerName} <DueStamp due={c.dueDate} /></span>
                        )}
                        {c.status === 'ON_HOLD_SHELF' && c.heldForName && (
                          <span className="cell-pair">For {c.heldForName}<span className="cell-sub">until {formatDate(c.holdExpiresOn)}</span></span>
                        )}
                        {!['ON_LOAN', 'ON_HOLD_SHELF'].includes(c.status) && <span className="cell-sub-inline">{c.note || '—'}</span>}
                      </td>
                      <td className="cell-date">{formatDate(c.acquiredOn)}</td>
                      <td className="actions">
                        <button className="btn btn-ghost btn-sm" onClick={() => setEditingCopy(c)}>Edit</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <AddCopies bookId={book.id} defaultShelf={copies[0]?.shelfLocation || ''} onAdded={(added) => {
              toast(`Added ${added.map((c) => c.barcode).join(', ')}`);
              reload();
            }} />
          </section>

          <section className="card">
            <div className="card-header">
              <h2>Holds queue</h2>
              <span className="card-meta">{queue.length ? plural(queue.length, 'member') : 'Nobody waiting'}</span>
            </div>
            {queue.length === 0 ? (
              <p className="card-note">When every copy is out, members can join the queue. Returned copies go to the first in line automatically.</p>
            ) : (
              <ol className="queue">
                {queue.map((h) => (
                  <li key={h.id}>
                    <span className="queue-pos">{h.status === 'READY' ? '—' : ordinal(h.queuePosition)}</span>
                    <span className="queue-member">
                      <Link to={`/members/${h.memberId}`}>{h.memberName}</Link>
                      <span className="cell-sub">{h.cardNumber} · placed {formatDateTime(h.placedAt)}</span>
                    </span>
                    <HoldStatus status={h.status} />
                    {h.status === 'READY' && <span className="cell-sub">{h.barcode} until {formatDate(h.expiresOn)}</span>}
                  </li>
                ))}
              </ol>
            )}
          </section>
        </div>
      </div>

      {editing && (
        <BookFormModal
          book={book}
          categories={categories}
          onClose={() => setEditing(false)}
          onSaved={(saved) => {
            toast(`"${saved.title}" updated`);
            setEditing(false);
            reload();
          }}
        />
      )}

      {editingCopy && (
        <CopyModal copy={editingCopy} onClose={() => setEditingCopy(null)} onSaved={(saved) => {
          toast(`${saved.barcode} is now ${COPY_STATUS_LABELS[saved.status].toLowerCase()}`);
          setEditingCopy(null);
          reload();
        }} />
      )}

      {deleting && (
        <ConfirmDialog
          title="Delete this title?"
          message={`"${book.title}" and its ${plural(copies.length, 'copy', 'copies')} will be removed. Titles that have ever been lent can't be deleted; withdraw their copies instead.`}
          confirmLabel="Delete"
          danger
          busy={deleteBusy}
          onConfirm={handleDelete}
          onCancel={() => setDeleting(false)}
        />
      )}
    </>
  );
}

function AddCopies({ bookId, defaultShelf, onAdded }) {
  const [count, setCount] = useState(1);
  const [shelf, setShelf] = useState(defaultShelf);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      onAdded(await booksApi.addCopies(bookId, { count: Number(count), shelfLocation: shelf }));
      setCount(1);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="inline-form" onSubmit={submit}>
      <label className="field field-narrow">
        <span>Add copies</span>
        <input type="number" min={1} max={200} value={count} onChange={(e) => setCount(e.target.value)} />
      </label>
      <label className="field">
        <span>Shelf</span>
        <input value={shelf} onChange={(e) => setShelf(e.target.value)} placeholder="Bay A2" />
      </label>
      <button className="btn btn-secondary" disabled={busy}><Icon name="plus" />{busy ? 'Adding…' : 'Add'}</button>
      {error && <span className="field-error inline-error">{error}</span>}
    </form>
  );
}

function CopyModal({ copy, onClose, onSaved }) {
  const locked = !MANUAL_STATUSES.includes(copy.status);
  const [form, setForm] = useState({ status: copy.status, shelfLocation: copy.shelfLocation || '', note: copy.note || '' });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const update = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }));

  const submit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setFormError('');
    try {
      onSaved(await copiesApi.update(copy.id, form));
    } catch (err) {
      setErrors(getFieldErrors(err));
      setFormError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      title={`Copy ${copy.barcode}`}
      onClose={onClose}
      width={460}
      footer={
        <>
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button type="submit" form="copy-form" className="btn btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Save'}</button>
        </>
      }
    >
      <form id="copy-form" className="form-stack" onSubmit={submit}>
        {formError && <div className="alert alert-error">{formError}</div>}
        <label className="field">
          <span>Status</span>
          <select value={form.status} onChange={update('status')} disabled={locked}>
            {(locked ? [copy.status] : MANUAL_STATUSES).map((s) => <option key={s} value={s}>{COPY_STATUS_LABELS[s]}</option>)}
          </select>
          {locked && <small className="form-hint">This copy is {COPY_STATUS_LABELS[copy.status].toLowerCase()}. Its status changes at the desk.</small>}
          {!locked && form.status === 'AVAILABLE' && copy.status !== 'AVAILABLE' && (
            <small className="form-hint">If someone is waiting for this title, the copy goes to the hold shelf for them instead.</small>
          )}
        </label>
        <label className="field">
          <span>Shelf</span>
          <input value={form.shelfLocation} onChange={update('shelfLocation')} placeholder="Bay A2" />
          {errors.shelfLocation && <small className="field-error">{errors.shelfLocation}</small>}
        </label>
        <label className="field">
          <span>Note</span>
          <input value={form.note} onChange={update('note')} placeholder="Water damage, spine split" />
        </label>
      </form>
    </Modal>
  );
}
