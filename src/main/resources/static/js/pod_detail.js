// DOM Elements
const podNameEl = document.getElementById("pod-name");
const podPhaseBadge = document.getElementById("pod-phase-badge");
const nodeLinkEl = document.getElementById("node-link");
const podIpEl = document.getElementById("pod-ip");
const podAgeEl = document.getElementById("pod-age");
const backBtn = document.getElementById("back-btn");

const metaNamespace = document.getElementById("meta-namespace");
const specSa = document.getElementById("spec-sa");
const statusQos = document.getElementById("status-qos");
const specRestart = document.getElementById("spec-restart");
const specPriority = document.getElementById("spec-priority");

const containersList = document.getElementById("containers-list");
const conditionsTableBody = document.getElementById("conditions-table-body");
const labelsContainer = document.getElementById("labels-container");
const annotationsContainer = document.getElementById("annotations-container");

const formatBytes = (bytes, decimals = 1) => {
  if (!bytes) return "0 B";
  const k = 1024;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = ["B", "KiB", "MiB", "GiB", "TiB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + " " + sizes[i];
};

const formatCpu = (milli) => {
  if (!milli) return "0m";
  return milli + "m";
};

const renderDetail = (data) => {
  // Header
  podNameEl.textContent = data.name;

  // Phase Badge
  podPhaseBadge.textContent = data.phase;
  let phaseClass = "text-slate-400 border-slate-700 bg-slate-800";
  if (data.phase === "Running")
    phaseClass = "text-emerald-400 border-emerald-500/30 bg-emerald-500/10";
  else if (data.phase === "Pending")
    phaseClass = "text-amber-400 border-amber-500/30 bg-amber-500/10";
  else if (data.phase === "Failed")
    phaseClass = "text-rose-400 border-rose-500/30 bg-rose-500/10";
  podPhaseBadge.className = `text-xs px-2 py-1 rounded-full border ${phaseClass}`;

  // Node Link
  if (data.nodeName && data.nodeName !== "-") {
    nodeLinkEl.textContent = data.nodeName;
    nodeLinkEl.href = `/cluster/nodes/${data.nodeName}`;
    // Setup back button logic
    backBtn.onclick = () =>
      (window.location.href = `/cluster/nodes/${data.nodeName}`);
  } else {
    nodeLinkEl.textContent = "N/A";
    nodeLinkEl.removeAttribute("href");
    backBtn.onclick = () => (window.location.href = "/cluster/nodes");
  }

  podIpEl.textContent = data.podIp;
  // Age calculation
  if (data.creationTimestamp) {
    // Just show date for simplicity or could calc relative time
    podAgeEl.textContent = data.creationTimestamp
      .replace("T", " ")
      .split(".")[0];
  }

  // Right Column Details
  metaNamespace.textContent = data.namespace;
  specSa.textContent = data.serviceAccountName;
  statusQos.textContent = data.qosClass;
  specRestart.textContent = data.restartPolicy;
  specPriority.textContent = data.priorityClassName;

  // Labels
  labelsContainer.innerHTML = "";
  if (data.labels) {
    Object.entries(data.labels).forEach(([key, value]) => {
      const span = document.createElement("span");
      span.className =
        "text-[10px] bg-slate-800 text-slate-400 px-2 py-1 rounded border border-slate-700";
      span.textContent = `${key}=${value}`;
      labelsContainer.appendChild(span);
    });
  }

  // Annotations
  annotationsContainer.innerHTML = "";
  if (data.annotations) {
    Object.entries(data.annotations).forEach(([key, value]) => {
      const div = document.createElement("div");
      div.className =
        "flex flex-col border-b border-slate-800 pb-1 last:border-0";
      div.innerHTML = `<span class="text-slate-500">${key}</span><span class="text-slate-300 ml-2">${value}</span>`;
      annotationsContainer.appendChild(div);
    });
  }

  // Containers
  containersList.innerHTML = "";
  data.containers.forEach((c) => {
    const div = document.createElement("div");
    div.className = "border border-slate-700 bg-slate-800/30 rounded-lg p-4";

    let stateColor = "text-slate-400";
    if (c.state === "Running") stateColor = "text-emerald-400";
    else if (c.state === "Waiting") stateColor = "text-amber-400";
    else if (c.state === "Terminated") stateColor = "text-rose-400";

    let portsHtml = "";
    if (c.ports && c.ports.length > 0) {
      portsHtml =
        `<div class="mt-2 text-xs text-slate-400">Ports: ` +
        c.ports.map((p) => `${p.containerPort}/${p.protocol}`).join(", ") +
        `</div>`;
    }

    div.innerHTML = `
        <div class="flex items-start justify-between mb-2">
            <div>
                <h4 class="font-bold text-white flex items-center gap-2">
                    <i class="fas fa-box text-sky-500"></i> ${c.name}
                </h4>
                <p class="text-xs text-slate-400 mt-1">Image: ${c.image}</p>
            </div>
            <div class="text-right">
                <span class="text-sm font-medium ${stateColor}">${
      c.state
    }</span>
                <p class="text-xs text-slate-500">Restarts: ${
                  c.restartCount
                }</p>
            </div>
        </div>
        ${
          c.statemessage
            ? `<div class="text-xs text-rose-400 mb-2">${c.stateReason}: ${c.stateMessage}</div>`
            : ""
        }
        
        <div class="grid grid-cols-2 gap-4 mt-3 bg-slate-900/50 p-2 rounded">
            <div>
                <p class="text-[10px] text-slate-500 uppercase">CPU Request / Limit</p>
                <p class="text-sm text-slate-300">${formatCpu(
                  c.requestCpuMilliCores
                )} / ${formatCpu(c.limitCpuMilliCores)}</p>
            </div>
            <div>
                <p class="text-[10px] text-slate-500 uppercase">Memory Request / Limit</p>
                <p class="text-sm text-slate-300">${formatBytes(
                  c.requestMemoryBytes
                )} / ${formatBytes(c.limitMemoryBytes)}</p>
            </div>
        </div>
        ${portsHtml}
      `;
    containersList.appendChild(div);
  });

  // Conditions
  conditionsTableBody.innerHTML = "";
  data.conditions.forEach((cond) => {
    const tr = document.createElement("tr");
    tr.className = "border-b border-slate-700/50";

    let statusClass = "text-slate-400";
    if (cond.status === "True") statusClass = "text-emerald-400";

    tr.innerHTML = `
        <td class="px-4 py-2 font-medium">${cond.type}</td>
        <td class="px-4 py-2 ${statusClass}">${cond.status}</td>
        <td class="px-4 py-2 text-xs">${
          cond.lastTransitionTime
            ? cond.lastTransitionTime.replace("T", " ").split(".")[0]
            : "-"
        }</td>
        <td class="px-4 py-2 text-xs">${cond.reason || "-"}</td>
     `;
    conditionsTableBody.appendChild(tr);
  });
};

const fetchPodDetail = async () => {
  try {
    if (!currentPodName || currentPodName === "unknown") return;
    const res = await fetch(`/api/cluster/pods/${currentPodName}`);
    if (!res.ok) throw new Error("Failed to fetch pod");
    const data = await res.json();
    renderDetail(data);
  } catch (e) {
    console.error(e);
  }
};

document.addEventListener("DOMContentLoaded", fetchPodDetail);
