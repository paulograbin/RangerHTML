<template id="hello-world">
  <a href="/api/files">Files</a>

  <app-frame>
    <div class="container py-5">
      <div class="row">

        <canvas id="eventTimeChart" width="600" height="300"></canvas>

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
                  <span class="filename"></span>{{ file.creationDate }} / {{ file.group }} - {{ file.length }}
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

  async mounted() {
    console.log('mounted');

    try {
      const response = await fetch('/api/files');
      if (!response.ok) throw new Error('Failed to fetch documents.');

      this.files = await response.json();
    } catch (error) {
      console.error('Error fetching documents:', error);
    } finally {

      const timelineData = [];
      console.log(timelineData);

      this.files
          .filter(file => file.tombstone === false)
          .forEach(file => {
            timelineData.push(file.creationDate);
          });

      // Count events per hour (0-23)
      const hourCounts = Array(24).fill(0);
      timelineData.forEach(ts => {
        const date = new Date(ts.replace(/-/g, '/')); // Ensure parsing works in all browsers
        hourCounts[date.getHours()]++;
      });

      // Labels for hours (0-23 as 12 AM, 1 AM, ..., 11 PM)
      const hourLabels = Array.from({length: 24}, (_, i) => {
        if (i === 0) return '12 AM';
        if (i < 12) return `${i} AM`;
        if (i === 12) return '12 PM';
        return `${i - 12} PM`;
      });

      // Chart.js config
      const ctx = document.getElementById('eventTimeChart').getContext('2d');
      new Chart(ctx, {
        type: 'bar',
        data: {
          labels: hourLabels,
          datasets: [{
            label: 'Number of Events',
            data: hourCounts,
            backgroundColor: 'rgba(54, 162, 235, 0.5)',
            borderColor: 'rgba(54, 162, 235, 1)',
            borderWidth: 1
          }]
        },
        options: {
          responsive: true,
          scales: {
            y: {
              beginAtZero: true,
              title: {display: true, text: 'Number of Events'}
            },
            x: {
              title: {display: true, text: 'Hour of Day'}
            }
          },
          plugins: {
            title: {
              display: true,
              text: 'Distribution of Events by Hour of Day'
            },
            legend: {display: false}
          }
        }
      });
    }

    if (localStorage.showTombstone === 'true') {
      this.showTombstone = true;
    } else {
      this.showTombstone = false;
    }
  },

  async created() {
    console.log('created');

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
      localStorage.showTombstone = newValue;
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