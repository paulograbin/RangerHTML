<template id="hello-world">
  <a href="/api/files">Files</a>

  <app-frame>
    <div class="container py-5">
      <div class="row">
        <div class="col-lg-10 mx-auto">
          <h2 class="mb-4 text-center">Downloadable Files</h2>

          <!-- Search input -->
          <div class="mb-3">
            <input type="text" id="docSearch" v-model="searchQuery" class="form-control"
                   placeholder="Search documents...">
          </div>

          <!-- Checkbox for tombstone toggle -->
          <div class="col-md-4 d-flex align-items-center">
            <div class="form-check ms-3">
              <input class="form-check-input" type="checkbox" id="showTombstone" v-model="showTombstone">
              <label class="form-check-label" for="showTombstone">
                Show tombstone files
              </label>
            </div>
          </div>

          <!-- Checkbox for tombstone toggle -->
          <div class="col-md-4 d-flex align-items-center">
            <div class="form-check ms-3">
              <input class="form-check-input" type="checkbox" id="recentFilesOnly" v-model="recentFilesOnly">
              <label class="form-check-label" for="recentFilesOnly">
                Recent files only
              </label>
            </div>
          </div>

          <!-- Loading Spinner -->
          <div v-if="loading" class="text-center py-5">
            <div class="spinner-border text-primary" role="status" style="width: 4rem; height: 4rem;">
              <span class="visually-hidden">Loading...</span>
            </div>
          </div>

          <!-- No Files Found (Fancy Empty State) -->
          <div v-else-if="filteredFiles.length === 0" class="empty-state">
            <div class="icon">
              <i class="bi bi-file-earmark-x-fill"></i> <!-- Icon for empty state -->
            </div>
            <p>Oops, <span>no documents found</span> that match your search.</p>
          </div>

          <ul v-else class="list-group mb-4" id="documents-list">
            <li v-for="file in filteredFiles" :key="file.name" class="list-group-item file-entry">

              <div v-if="file.tombstone" class="d-flex justify-content-between align-items-center tombstone">
                  <span>
                    <i class="bi bi-info me-2"></i>
                    <span class="filename"> {{ file.start }} to {{ file.end }}</span>
                  </span>
              </div>

              <div v-if="!file.tombstone" class="d-flex justify-content-between align-items-center">
                <span><i class="bi bi-file-text-fill me-2"></i>
                  <span class="filename" v-html="file.creationDate"></span> / {{ file.group }} - {{ file.length }}
                </span>
                <a :href="`/file/${file.name}`" class="btn btn-sm btn-outline-primary">
                  <i class="bi bi-download me-1"></i>Download
                </a>
              </div>
            </li>

          </ul>
        </div>
      </div>
    </div>
  </app-frame>
</template>

<script setup>
app.component("hello-world", {
  template: "#hello-world",

  data() {
    console.log('data');
    return {
      files: [],
      searchQuery: "",
      showTombstone: false,
      recentFilesOnly: false,
      loading: true,
    }
  },

  computed: {
    filteredFiles() {
      return this.files.filter(file => {
        const matchesSearch = file.name.toLowerCase().includes(this.searchQuery.toLowerCase());
        if (!this.showTombstone && file.tombstone) {
          return false;
        }
        return matchesSearch;
      });
    }
  },

  mounted() {
    console.log('mounted');
  },

  async created() {
    console.log('created')

    try {
      const response = await fetch('/api/files');
      if (!response.ok) throw new Error('Failed to fetch documents.');

      this.files = await response.json();
    } catch (error) {
      console.error('Error fetching documents:', error);
    } finally {
      this.loading = false;  // <--- important! even if fetch fails
    }
  },

  watch: {
    showTombstone(newValue) {
      console.log('Show tombstone changed:', newValue);
    }
  }
});
</script>
<style>
.empty-state {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  padding: 4rem;
  background-color: #f7f7f7;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: background-color 0.3s ease;
}

.empty-state .icon {
  font-size: 4rem;
  color: #6c757d;
  margin-bottom: 1.5rem;
}

.empty-state p {
  font-size: 1.25rem;
  color: #6c757d;
  font-weight: bold;
}

.empty-state p span {
  color: #007bff;
  font-weight: bold;
}

.empty-state:hover {
  background-color: #e9ecef;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}
</style>