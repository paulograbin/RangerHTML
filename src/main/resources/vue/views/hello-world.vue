<template id="hello-world">
  <a href="/api/files">Files</a>

  <app-frame>
    <div class="container py-5">
      <div class="row">
        <div class="col-lg-10 mx-auto">
          <h2 class="mb-4 text-center">Downloadable Files</h2>

          <!-- Search input -->
          <div class="mb-3">
            <input type="text" id="docSearch" class="form-control" placeholder="Search documents...">
          </div>

          <ul class="list-group mb-4" id="documents-list">
            <li v-for="file in files" class="list-group-item file-entry">

                <div v-if="file.tombstone" class="d-flex justify-content-between align-items-center">
                  <span>
                    <i class="bi bi-info me-2"></i>
                    <span class="filename">{{ file.name }}</span>
                  </span>
                </div>

                <div v-if="!file.tombstone" class="d-flex justify-content-between align-items-center">
                  <span><i class="bi bi-file-text-fill me-2"></i><span class="filename">{{ file.name }} / {{ file.group }} - {{ file.length }}</span></span>
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

<script>
app.component("hello-world", {
  template: "#hello-world",
  data: () => ({
    files: [],
  }),
  mounted() {
    document.onreadystatechange = () => {
      document.getElementById('docSearch').addEventListener('input', function () {
        const query = this.value.toLowerCase();
        document.querySelectorAll('#documents-list .file-entry').forEach(li => {
          const filename = li.querySelector('.filename').textContent.toLowerCase();
          li.style.display = filename.includes(query) ? '' : 'none';
        });
      });
    }
  },
  created() {
    fetch("/api/files")
        .then(response => response.json())
        .then(json => this.files = json)
        .catch(error => alert(error))
  },
});
</script>
<style>
</style>