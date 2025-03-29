
// js/git.js
document.addEventListener('DOMContentLoaded', function() {
    const analyzeBtn = document.getElementById('analyze-btn');
    const repoUrlInput = document.getElementById('repo-url');
    const usernameInput = document.getElementById('git-username');
    const tokenInput = document.getElementById('git-token');
    const autoSyncCheckbox = document.getElementById('auto-sync');

    async function analyzeRepository() {
        const repoUrl = repoUrlInput.value.trim();
        const username = usernameInput.value.trim();
        const token = tokenInput.value.trim();
        const autoSync = autoSyncCheckbox.checked;

        if (!repoUrl || !username || !token) {
            alert('Please fill in all required fields');
            return;
        }

        try {
            const response = await fetch('http://localhost:8090/api/v1/ollama/analyze_git_repository', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: `repoUrl=${encodeURIComponent(repoUrl)}&userName=${encodeURIComponent(username)}&token=${encodeURIComponent(token)}`
            });

            const result = await response.json();
            if (result.code === '0000') {
                alert('Git repository analysis successful');
                repoUrlInput.value = '';
                usernameInput.value = '';
                tokenInput.value = '';
                autoSyncCheckbox.checked = false;
            } else {
                alert('Git repository analysis failed: ' + result.info);
            }
        } catch (error) {
            console.error('Git analysis error:', error);
            alert('Git repository analysis failed');
        }
    }

    // Event listener
    analyzeBtn.addEventListener('click', analyzeRepository);
});