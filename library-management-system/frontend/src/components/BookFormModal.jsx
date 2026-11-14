import { useState } from 'react';
import Modal from './Modal';
import { booksApi } from '../api/services';
import { getErrorMessage, getFieldErrors } from '../api/client';

const EMPTY = { title: '', author: '', isbn: '', category: '', publishedYear: '', description: '', initialCopies: 1, shelfLocation: '' };

/** Add a new title with its first copies (book = null), or edit a title's details. Copies are managed on the title's page. */
export default function BookFormModal({ book, categories = [], onClose, onSaved }) {
  const isEdit = !!book;
  const [form, setForm] = useState(() =>
    book
      ? { ...EMPTY, ...book, category: book.category || '', publishedYear: book.publishedYear ?? '', description: book.description || '' }
      : EMPTY
  );
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const update = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setErrors({});
    setFormError('');
    const payload = {
      title: form.title,
      author: form.author,
      isbn: form.isbn,
      category: form.category,
      publishedYear: form.publishedYear === '' ? null : Number(form.publishedYear),
      description: form.description,
      initialCopies: isEdit ? null : Number(form.initialCopies),
      shelfLocation: isEdit ? null : form.shelfLocation,
    };
    try {
      const saved = isEdit ? await booksApi.update(book.id, payload) : await booksApi.create(payload);
      onSaved(saved, isEdit);
    } catch (err) {
      setErrors(getFieldErrors(err));
      setFormError(getErrorMessage(err, 'Could not save the book'));
    } finally {
      setSaving(false);
    }
  };

  const field = (name, label, props = {}) => (
    <label className={`field ${props.full ? 'field-full' : ''}`}>
      <span>{label}</span>
      <input value={form[name]} onChange={update(name)} className={errors[name] ? 'invalid' : ''} {...props.input} />
      {errors[name] && <small className="field-error">{errors[name]}</small>}
    </label>
  );

  return (
    <Modal
      title={isEdit ? 'Edit title' : 'Add a title'}
      onClose={onClose}
      footer={
        <>
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button type="submit" form="book-form" className="btn btn-primary" disabled={saving}>
            {saving ? 'Saving…' : isEdit ? 'Save changes' : 'Add title'}
          </button>
        </>
      }
    >
      <form id="book-form" className="form-grid" onSubmit={handleSubmit} noValidate>
        {formError && <div className="alert alert-error field-full">{formError}</div>}
        {field('title', 'Title', { full: true, input: { required: true, autoFocus: true } })}
        {field('author', 'Author', { full: true, input: { required: true } })}
        {field('isbn', 'ISBN', { input: { required: true, placeholder: '978-0132350884' } })}
        <label className="field">
          <span>Category</span>
          <input value={form.category} onChange={update('category')} list="category-options" placeholder="Programming" />
          <datalist id="category-options">
            {categories.map((c) => <option key={c} value={c} />)}
          </datalist>
        </label>
        {field('publishedYear', 'Published year', { input: { type: 'number', min: 1000, max: 2100 } })}
        {!isEdit && field('initialCopies', 'Copies to add', { input: { type: 'number', min: 0, max: 200 } })}
        {!isEdit && field('shelfLocation', 'Shelf', { full: true, input: { placeholder: 'Bay A2' } })}
        <label className="field field-full">
          <span>Description</span>
          <textarea rows={3} value={form.description} onChange={update('description')} />
        </label>
        {!isEdit && (
          <p className="form-hint field-full">Each copy gets its own barcode. Print the labels from the title's page after adding it.</p>
        )}
      </form>
    </Modal>
  );
}
