// js/upload.js
document.addEventListener('DOMContentLoaded', function() {
    const knowledgeList = document.getElementById('knowledge-list');
    const refreshBtn = document.getElementById('refresh-btn');
    const uploadBtn = document.getElementById('upload-btn');
    const tagNameInput = document.getElementById('tag-name');
    const fileInput = document.getElementById('file-input');

    async function loadKnowledgeBases() {
        try {
            const response = await fetch('http://localhost:8090/api/v1/ollama/knowledge/tags');
            const result = await response.json();
            if (result.code === '0000' && result.data) {
                knowledgeList.innerHTML = '';
                result.data.forEach(tag => {
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td>${tag.tagName}</td>
                        <td>${tag.source || 'FILE'}</td>
                        <td>${tag.lastUpdateTime || '-'}</td>
                        <td>
                            <button class="btn btn-secondary mr-2 delete-btn" data-tag="${tag.tagName}">
                                Delete
                            </button>
                        </td>
                    `;
                    knowledgeList.appendChild(row);
                });
            }
        } catch (error) {
            console.error('Error loading knowledge bases:', error);
            alert('Failed to load knowledge bases');
        }
    }

    async function handleFileUpload() {
        const tagName = tagNameInput.value.trim();
        const files = fileInput.files;

        if (!tagName || !files.length) {
            alert('Please enter tag name and select files');
            return;
        }

        const formData = new FormData();
        formData.append('ragTag', tagName);
        for (let file of files) {
            formData.append('file', file);
        }

        try {
            const response = await fetch('http://localhost:8090/api/v1/ollama/file/upload', {
                method: 'POST',
                body: formData
            });
            
            const result = await response.json();
            if (result.code === '0000') {
                alert('Upload successful');
                tagNameInput.value = '';
                fileInput.value = '';
                await loadKnowledgeBases();
            } else {
                alert('Upload failed: ' + result.info);
            }
        } catch (error) {
            console.error('Upload error:', error);
            alert('Upload failed');
        }
    }

    // Event listeners
    refreshBtn.addEventListener('click', loadKnowledgeBases);
    uploadBtn.addEventListener('click', handleFileUpload);
    knowledgeList.addEventListener('click', async (e) => {
        if (e.target.classList.contains('delete-btn')) {
            const tagName = e.target.dataset.tag;
            if (confirm(`Are you sure you want to delete "${tagName}"?`)) {
                try {
                    const response = await fetch(`http://localhost:8090/api/v1/ollama/knowledge/tag/${tagName}`, {
                        method: 'DELETE'
                    });
                    const result = await response.json();
                    if (result.code === '0000') {
                        alert('Knowledge base deleted successfully');
                        await loadKnowledgeBases();
                    } else {
                        alert('Failed to delete knowledge base');
                    }
                } catch (error) {
                    console.error('Delete error:', error);
                    alert('Failed to delete knowledge base');
                }
            }
        }
    });

    // Initial load
    loadKnowledgeBases();
});