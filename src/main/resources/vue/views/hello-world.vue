<template id="hello-world">
  <a href="/api/files">Files</a>

  <app-frame>
    <div class="container py-5">
      <div class="row">
        <div class="col-lg-10 mx-auto">
          <h2 class="mb-4 text-center">Downloadable Files</h2>

          <!-- Search input -->
          <div class="mb-3">
            <input type="text" id="docSearch" v-model="searchQuery" class="form-control" placeholder="Search documents...">
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

          <ul class="list-group mb-4" id="documents-list">
            <li v-for="file in filteredFiles" :key="file.name"  class="list-group-item file-entry" >

              <div v-if="file.tombstone" class="d-flex justify-content-between align-items-center tombstone">
                  <span>
                    <i class="bi bi-info me-2"></i>
                    <span class="filename">{{ file.name }}</span>
                  </span>
              </div>

              <div v-if="!file.tombstone" class="d-flex justify-content-between align-items-center">
                <span><i class="bi bi-file-text-fill me-2"></i><span class="filename">{{ file.name }} / {{ file.group }} - {{file.length }}</span></span>
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
</style>