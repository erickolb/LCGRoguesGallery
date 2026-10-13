const viewer = document.querySelector('.screenshot-viewer');
const fullImage = viewer.querySelector('.screenshot-full');
const closeButton = viewer.querySelector('.screenshot-close');

document.querySelectorAll('.gallery figure > a').forEach((link) => {
  link.addEventListener('click', (event) => {
    // Keep the normal browser behavior for opening a separate tab or window.
    if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;

    event.preventDefault();
    fullImage.src = link.href;
    fullImage.alt = link.querySelector('img').alt;
    viewer.showModal();
    document.documentElement.classList.add('screenshot-open');
  });
});

closeButton.addEventListener('click', () => viewer.close());
viewer.addEventListener('click', (event) => {
  if (event.target === viewer) viewer.close();
});
viewer.addEventListener('close', () => {
  document.documentElement.classList.remove('screenshot-open');
  fullImage.removeAttribute('src');
  fullImage.alt = '';
});
