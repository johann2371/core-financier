<script setup>
import { computed } from 'vue'
import { Line } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler
} from 'chart.js'

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler
)

const props = defineProps({
  data: {
    type: Array,
    default: () => []
  }
})

const chartData = computed(() => ({
  labels: props.data.map(m => m.label),
  datasets: [
    {
      label: 'Encaissements',
      data: props.data.map(m => m.encaissements),
      borderColor: '#10b981',
      backgroundColor: 'rgba(16, 185, 129, 0.08)',
      pointBackgroundColor: '#10b981',
      pointBorderColor: '#fff',
      pointBorderWidth: 2,
      pointRadius: 5,
      pointHoverRadius: 7,
      borderWidth: 2.5,
      fill: true,
      tension: 0.4
    },
    {
      label: 'Décaissements',
      data: props.data.map(m => m.decaissements),
      borderColor: '#ef4444',
      backgroundColor: 'rgba(239, 68, 68, 0.08)',
      pointBackgroundColor: '#ef4444',
      pointBorderColor: '#fff',
      pointBorderWidth: 2,
      pointRadius: 5,
      pointHoverRadius: 7,
      borderWidth: 2.5,
      fill: true,
      tension: 0.4
    }
  ]
}))

const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  interaction: {
    mode: 'index',
    intersect: false
  },
  plugins: {
    legend: {
      position: 'bottom',
      labels: {
        usePointStyle: true,
        pointStyle: 'circle',
        padding: 20,
        font: { size: 12, weight: '600', family: 'Inter, sans-serif' },
        color: '#4b5563'
      }
    },
    tooltip: {
      backgroundColor: '#1e293b',
      titleFont: { size: 13, weight: '700', family: 'Inter, sans-serif' },
      bodyFont: { size: 12, family: 'Inter, sans-serif' },
      padding: 12,
      cornerRadius: 8,
      displayColors: true,
      boxPadding: 6,
      callbacks: {
        label: function (context) {
          return `${context.dataset.label}: ${context.parsed.y.toLocaleString('fr-FR')} XAF`
        }
      }
    }
  },
  scales: {
    x: {
      grid: { display: false },
      ticks: {
        font: { size: 11, weight: '600', family: 'Inter, sans-serif' },
        color: '#9ca3af'
      },
      border: { display: false }
    },
    y: {
      beginAtZero: true,
      grid: {
        color: '#f3f4f6',
        drawBorder: false
      },
      ticks: {
        font: { size: 11, family: 'Inter, sans-serif' },
        color: '#9ca3af',
        callback: function (value) {
          if (value >= 1_000_000) return (value / 1_000_000).toFixed(1) + 'M'
          if (value >= 1_000) return (value / 1_000).toFixed(0) + 'K'
          return value
        },
        maxTicksLimit: 6
      },
      border: { display: false }
    }
  }
}))
</script>

<template>
  <div class="flux-chart-wrapper">
    <Line :data="chartData" :options="chartOptions" />
  </div>
</template>

<style scoped>
.flux-chart-wrapper {
  height: 260px;
  width: 100%;
}
</style>
