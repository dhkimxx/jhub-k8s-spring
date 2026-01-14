import { createDoughnutChart } from "./components/chart.js";

// DOM Elements
const nodeNameEl = document.getElementById("node-name");
const nodeStatusBadge = document.getElementById("node-status-badge");
const nodeRoleEl = document.getElementById("node-role");
const nodeIpEl = document.getElementById("node-ip");
const nodeAgeEl = document.getElementById("node-age");

const sysOsImage = document.getElementById("sys-os-image");
const sysKernel = document.getElementById("sys-kernel");
const sysKubelet = document.getElementById("sys-kubelet");
const sysRuntime = document.getElementById("sys-runtime");
const sysArch = document.getElementById("sys-arch");

const labelsContainer = document.getElementById("labels-container");
const conditionsGrid = document.getElementById("conditions-grid");
const podsTableBody = document.getElementById("pods-table-body");
const podCountEl = document.getElementById("pod-count");

// Charts
let cpuChart, memChart, storageChart;

const formatNumber = (num, suffix = "") => {
  return num
    ? new Intl.NumberFormat("en-US", { maximumFractionDigits: 1 }).format(num) +
        suffix
    : "0" + suffix;
};

const formatBytes = (bytes, decimals = 1) => {
  if (!bytes) return "0 B";
  const k = 1024;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = ["B", "KiB", "MiB", "GiB", "TiB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + " " + sizes[i];
};

const renderDetail = (data) => {
  // Basic Info
  nodeNameEl.textContent = data.nodeName;
  nodeStatusBadge.textContent = data.status;
  if (data.status === "Ready") {
    nodeStatusBadge.className =
      "text-xs px-2 py-1 rounded-full border border-emerald-500/30 bg-emerald-500/10 text-emerald-400";
  } else {
    nodeStatusBadge.className =
      "text-xs px-2 py-1 rounded-full border border-rose-500/30 bg-rose-500/10 text-rose-400";
  }
  nodeRoleEl.textContent = data.role.toUpperCase();
  nodeIpEl.textContent = data.internalIp;

  // Calculate age roughly or use creation timestamp
  // data.creationTimestamp is string. Let's just show it.
  // Or simple parse
  // For now just show timestamp
  nodeAgeEl.textContent = data.creationTimestamp.split("T")[0]; // YYYY-MM-DD

  // System Info
  sysOsImage.textContent = data.osImage;
  sysKernel.textContent = data.kernelVersion;
  sysKubelet.textContent = data.kubeletVersion;
  sysRuntime.textContent = data.containerRuntimeVersion;
  sysArch.textContent = `${data.architecture} / ${data.operatingSystem}`;

  // Metrics
  document.getElementById("detail-cpu-percent").textContent =
    Math.round(data.cpuUsagePercent) + "%";
  document.getElementById("detail-cpu-req").textContent = formatNumber(
    data.requestedCpuMilliCores,
    "m"
  );
  document.getElementById("detail-cpu-alloc").textContent = formatNumber(
    data.allocatableCpuMilliCores,
    "m"
  );

  document.getElementById("detail-mem-percent").textContent =
    Math.round(data.memoryUsagePercent) + "%";
  document.getElementById("detail-mem-req").textContent = formatBytes(
    data.requestedMemoryBytes
  );
  document.getElementById("detail-mem-alloc").textContent = formatBytes(
    data.allocatableMemoryBytes
  );

  document.getElementById("detail-storage-percent").textContent =
    Math.round(data.ephemeralStorageUsagePercent) + "%";
  document.getElementById("detail-storage-req").textContent = formatBytes(
    data.requestedEphemeralStorageBytes
  );
  document.getElementById("detail-storage-alloc").textContent = formatBytes(
    data.allocatableEphemeralStorageBytes
  );

  // Charts
  if (cpuChart) cpuChart.destroy();
  if (memChart) memChart.destroy();
  if (storageChart) storageChart.destroy();

  cpuChart = createDoughnutChart(
    "detail-cpu-chart",
    data.cpuUsagePercent,
    "#10b981"
  );
  memChart = createDoughnutChart(
    "detail-mem-chart",
    data.memoryUsagePercent,
    "#0ea5e9"
  );
  storageChart = createDoughnutChart(
    "detail-storage-chart",
    data.ephemeralStorageUsagePercent,
    "#f59e0b"
  );

  // Labels
  labelsContainer.innerHTML = "";
  Object.entries(data.labels).forEach(([key, value]) => {
    const span = document.createElement("span");
    span.className =
      "text-[10px] bg-slate-800 text-slate-400 px-2 py-1 rounded border border-slate-700";
    span.textContent = `${key}=${value}`;
    labelsContainer.appendChild(span);
  });

  // Conditions
  conditionsGrid.innerHTML = "";
  data.conditions.forEach((cond) => {
    const div = document.createElement("div");
    const isGood = cond.status === "False"; // For pressure conditions, False is good. For Ready, True is good.
    // Actually typical conditions: MemoryPressure, DiskPressure, PIDPressure, Ready.
    // Logic:
    let colorClass = "text-slate-400 border-slate-700 bg-slate-800";
    let icon = "fas fa-check";

    if (cond.type === "Ready") {
      if (cond.status === "True") {
        colorClass = "text-emerald-400 border-emerald-500/30 bg-emerald-500/10";
        icon = "fas fa-check-circle";
      } else {
        colorClass = "text-rose-400 border-rose-500/30 bg-rose-500/10";
        icon = "fas fa-times-circle";
      }
    } else {
      // Pressure conditions (DiskPressure, MemoryPressure, PIDPressure)
      if (cond.status === "True") {
        colorClass = "text-rose-400 border-rose-500/30 bg-rose-500/10";
        icon = "fas fa-exclamation-circle";
      } else {
        colorClass = "text-slate-400 border-slate-700 bg-slate-800"; // Normal
        icon = "fas fa-check";
      }
    }

    div.className = `p-3 rounded-lg border flex items-center justify-between ${colorClass}`;
    div.innerHTML = `
        <div>
            <p class="font-medium text-sm">${cond.type}</p>
            <p class="text-xs opacity-70">${
              cond.message || cond.reason || "-"
            }</p>
        </div>
        <i class="${icon}"></i>
      `;
    conditionsGrid.appendChild(div);
  });

  // Pods List
  podsTableBody.innerHTML = "";
  podCountEl.textContent = data.pods.length;
  data.pods.forEach((pod) => {
    const tr = document.createElement("tr");
    tr.className =
      "border-b border-slate-700/50 hover:bg-slate-800/50 cursor-pointer transition-colors";
    tr.onclick = () => (location.href = `/cluster/pods/${pod.name}`);

    let statusColor = "text-slate-400";
    if (pod.status === "Running") statusColor = "text-emerald-400";
    else if (pod.status === "Pending") statusColor = "text-amber-400";
    else if (pod.status === "Failed") statusColor = "text-rose-400";

    tr.innerHTML = `
        <td class="px-4 py-3 font-medium text-white">
            ${pod.name}
        </td>
        <td class="px-4 py-3">${pod.namespace}</td>
        <td class="px-4 py-3 ${statusColor}">${pod.status}</td>
        <td class="px-4 py-3">${formatNumber(
          pod.requestedCpuMilliCores,
          "m"
        )}</td>
        <td class="px-4 py-3">${formatBytes(pod.requestedMemoryBytes)}</td>
        <td class="px-4 py-3 text-center">
          <button 
            class="text-slate-400 hover:text-pink-400 transition-colors p-1.5 hover:bg-slate-800 rounded"
            onclick="location.href='/cluster/pods/${encodeURIComponent(
              pod.name
            )}'"
            title="View details"
          >
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M13.5 4.5L21 12m0 0l-7.5 7.5M21 12H3" />
            </svg>
          </button>
        </td>
      `;
    podsTableBody.appendChild(tr);
  });
};

const fetchNodeDetail = async () => {
  try {
    if (!currentNodeName || currentNodeName === "unknown") return;

    const res = await fetch(`/api/cluster/nodes/${currentNodeName}`);
    if (!res.ok) throw new Error("Failed to fetch node detail");
    const data = await res.json();
    renderDetail(data);
  } catch (e) {
    console.error(e);
    // Show error UI?
  }
};

// Init
document.addEventListener("DOMContentLoaded", () => {
  fetchNodeDetail();
  // Refresh every 30s?
});
