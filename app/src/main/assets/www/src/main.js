import{createZip,safeArchiveName}from"./zip.js";import{clampClipRange,isClipRangeAllowed,nextStickerId}from"./editor-utils.js";import{chooseOptimalFps,normalizeExportFps}from"./fps-utils.js";
const $=id=>document.getElementById(id),MAX=10,MAX_ITEMS=30,MAX_BYTES=150*1024*1024,MAX_PACK_BYTES=75*1024*1024;
const s={media:null,mediaUrl:null,kind:null,audio:null,audioUrl:null,duration:0,pack:[],nextId:1,loadToken:0,rendering:false},video=$("videoPreview"),image=$("imagePreview"),player=$("audioPreview"),wave=$("waveform"),ctx=wave.getContext("2d");let mixedPreview=null,mixedPreviewUrl=null,previewMainGain=1,previewMixGain=.8;
function toast(message,error=false){const el=document.createElement("div");el.className="toast"+(error?" error":"");el.textContent=message;$("toast").append(el);requestAnimationFrame(()=>el.classList.add("visible"));setTimeout(()=>{el.classList.remove("visible");setTimeout(()=>el.remove(),220)},3500)}
function t(v){v=Number.isFinite(v)&&v>0?v:0;return Math.floor(v/60)+":"+String(Math.floor(v%60)).padStart(2,"0")}
function size(v){return v<1048576?(v/1024).toFixed(1)+" KB":(v/1048576).toFixed(1)+" MB"}
function isVideo(f){return f.type.startsWith("video/")||/\.(mp4|mov|m4v|webm|3gp)$/i.test(f.name)}
function isImage(f){return f.type.startsWith("image/")||/\.(gif|webp|png|jpe?g|avif)$/i.test(f.name)}
function wait(el,event,timeout=15000){return new Promise((resolve,reject)=>{const timer=setTimeout(()=>{el.removeEventListener(event,done);reject(new Error("Tempo esgotado ao carregar a mídia."))},timeout);function done(e){clearTimeout(timer);resolve(e)}el.addEventListener(event,done,{once:true})})}
function revoke(){if(s.mediaUrl)URL.revokeObjectURL(s.mediaUrl);if(s.audioUrl)URL.revokeObjectURL(s.audioUrl);stopMixedPreview();player.pause();player.removeAttribute("src");player.load();s.mediaUrl=s.audioUrl=null}
function drawWave(progress=0){ctx.clearRect(0,0,wave.width,wave.height);const count=64,gap=5,w=(wave.width-gap*(count-1))/count,seed=(s.audio||s.media)?.size||17;for(let i=0;i<count;i++){const h=8+Math.abs(Math.sin((i+seed%19)*.31)*Math.cos(i*.14))*44;ctx.fillStyle=i/count<=progress?"#9b8cff":"rgba(171,164,211,.28)";ctx.fillRect(i*(w+gap),(wave.height-h)/2,w,h)}}
function syncPlayer(){const d=Number.isFinite(player.duration)?player.duration:0,c=Number.isFinite(player.currentTime)?player.currentTime:0,p=d?c/d:0;$("audioSeek").value=p*100;$("currentTime").textContent=t(c);$("audioDuration").textContent=t(d);$("playAudio").textContent=player.paused?"▶":"Ⅱ";$("playAudio").setAttribute("aria-label",player.paused?"Reproduzir áudio":"Pausar áudio");$("muteAudio").textContent=player.muted?"×":"◖";drawWave(p)}
function stopMixedPreview(){if(mixedPreview){mixedPreview.pause();mixedPreview.removeAttribute("src");mixedPreview.load()}if(mixedPreviewUrl)URL.revokeObjectURL(mixedPreviewUrl);mixedPreview=mixedPreviewUrl=null}
function applyPreviewVolume(){const value=Math.max(0,Math.min(1,Number($("volume").value)||0));player.volume=Math.min(1,value*previewMainGain);if(mixedPreview)mixedPreview.volume=Math.min(1,value*previewMixGain)}
function syncMixedPreviewTime(){if(!mixedPreview||!Number.isFinite(player.currentTime))return;try{if(Math.abs(mixedPreview.currentTime-player.currentTime)>.2)mixedPreview.currentTime=player.currentTime}catch{}}
function syncMixedPreviewPlayback(){if(!mixedPreview)return;syncMixedPreviewTime();if(player.paused){mixedPreview.pause();return}mixedPreview.play().catch(()=>{})}
function setPlayer(){
 stopMixedPreview();player.pause();if(s.audioUrl)URL.revokeObjectURL(s.audioUrl);s.audioUrl=null;
 const mode=$("audioMode").value;let file=null;previewMainGain=1;previewMixGain=.8;
 if(!s.media)file=s.audio;
 else if(s.kind==="video"){
  if(mode==="replace")file=s.audio;
  else{file=s.media;if(mode==="mix"){previewMainGain=.65;if(s.audio){mixedPreview=document.createElement("audio");mixedPreview.preload="auto";mixedPreviewUrl=URL.createObjectURL(s.audio);mixedPreview.src=mixedPreviewUrl;mixedPreview.load()}}}
 }else if(mode!=="original")file=s.audio;
 if(file){s.audioUrl=URL.createObjectURL(file);player.src=s.audioUrl;player.load()}else{player.removeAttribute("src");player.load()}
 player.muted=false;if(mixedPreview)mixedPreview.muted=false;applyPreviewVolume();
 const ready=!!file;$("playAudio").disabled=$("audioSeek").disabled=$("muteAudio").disabled=!ready;
 let label="Selecione uma mídia com som ou adicione um áudio.";
 if(!s.media&&s.audio)label="Áudio personalizado selecionado";
 else if(s.kind==="video"&&mode==="original")label="Áudio original do vídeo";
 else if(s.kind==="video"&&mode==="replace")label=ready?"Áudio personalizado (substitui o original)":"Selecione um áudio para substituir o original.";
 else if(s.kind==="video"&&mode==="mix")label=s.audio?"Mixagem: áudio original + áudio personalizado":"Somente áudio original (modo mix)";
 else if(s.kind==="image"&&s.audio&&mode!=="original")label="Áudio personalizado da imagem";
 else if(s.kind==="image")label="Adicione um áudio para a imagem.";
 $("audioSource").textContent=label;$("audioSource").classList.toggle("ready",ready);syncPlayer()
}
function range(){return clampClipRange(Number($("trimStart").value),Number($("trimEnd").value),s.duration)||{start:NaN,end:NaN}}
function updateRange(){
 if(!s.media){$("addToPack").disabled=true;return}
 const{start,end}=range(),valid=isClipRangeAllowed(start,end,s.duration,MAX);
 if(valid){$("trimStart").value=String(start);$("trimEnd").value=String(end)}
 $("trimStart").max=Math.max(0,s.duration-.1);$("trimEnd").max=s.duration;
 $("durationBadge").textContent=(valid?t(end-start):"—")+" / 0:10";
 $("trimNote").textContent=valid?"Trecho selecionado: "+(end-start).toFixed(1)+" s. Máximo: 10 s.":"A mídia precisa ter pelo menos 0,1 s.";
 $("addToPack").disabled=!valid||s.rendering||s.pack.length>=MAX_ITEMS
}
async function loadMedia(file,token){
 if(token!==s.loadToken)return false;
 if(file.size>MAX_BYTES)throw Error("O arquivo ultrapassa 150 MB.");
 if(!isVideo(file)&&!isImage(file))throw Error("Selecione um vídeo ou imagem compatível.");
 revoke();s.media=file;s.kind=isVideo(file)?"video":"image";s.duration=0;s.mediaUrl=URL.createObjectURL(file);
 $("placeholder").hidden=true;video.hidden=image.hidden=true;$("previewType").textContent=s.kind==="video"?"VÍDEO":"IMAGEM";$("mediaName").textContent=file.name;$("mediaInfo").textContent=size(file.size);$("soundMark").hidden=s.kind!=="video";
 if(s.kind==="video"){
  const loaded=wait(video,"loadedmetadata");video.src=s.mediaUrl;video.load();await loaded;
  if(token!==s.loadToken)return false;
  const duration=Number.isFinite(video.duration)?video.duration:0;if(!duration)throw Error("Não foi possível detectar a duração do vídeo.");
  s.duration=duration;video.hidden=false;video.play().catch(()=>{});
 }else{
  image.src=s.mediaUrl;await image.decode().catch(()=>{});
  if(token!==s.loadToken)return false;
  s.duration=5;image.hidden=false
 }
 $("fileSummary").hidden=false;$("fileSummaryText").textContent=file.name+" · "+size(file.size);$("trimStart").value=0;$("trimEnd").value=Math.min(s.duration,MAX);$("audioMode").value=s.kind==="image"||s.audio?"replace":"original";setPlayer();updateRange();$("trimNote").textContent=s.kind==="image"?"Imagem estática: será criado um clipe com o áudio selecionado.":"Duração detectada: "+s.duration.toFixed(1)+" s.";
 return true
}
function clearMedia(){s.loadToken+=1;$("mediaInput").value="";$("audioInput").value="";video.pause();video.removeAttribute("src");video.load();image.removeAttribute("src");revoke();Object.assign(s,{media:null,kind:null,duration:0,audio:null});$("fileSummary").hidden=true;$("placeholder").hidden=false;video.hidden=image.hidden=$("soundMark").hidden=true;$("previewType").textContent="AGUARDANDO MÍDIA";$("mediaName").textContent="Nenhum arquivo selecionado";$("mediaInfo").textContent="—";$("trimStart").value=0;$("trimEnd").value=5;setPlayer();updateRange()}
function recorderType(){
 if(typeof MediaRecorder==="undefined")throw Error("Este navegador não oferece MediaRecorder. Atualize o WebView.");
 return["video/mp4;codecs=avc1.42E01E,mp4a.40.2","video/mp4","video/webm;codecs=vp9,opus","video/webm;codecs=vp8,opus","video/webm"].find(x=>MediaRecorder.isTypeSupported?.(x))||""
}
function configuredFps(){
 const value=$("fpsMode").value;
 if(value==="auto")return null;
 const fps=normalizeExportFps(value);
 if(fps===null)throw Error("Selecione um FPS válido.");
 return fps
}
async function seekVideo(video,time){
 if(Math.abs(video.currentTime-time)<.01)return;
 const sought=wait(video,"seeked",8000);
 video.currentTime=time;
 await sought
}
function estimatePresentedFps(sourceVideo){
 if(typeof sourceVideo.requestVideoFrameCallback!=="function")return Promise.resolve(30);
 return new Promise(resolve=>{
  const mediaTimes=[];
  const startedAt=performance.now();
  let callbackId=null,timer=0,settled=false;
  const finish=()=>{
   if(settled)return;
   settled=true;
   clearTimeout(timer);
   if(callbackId!==null&&typeof sourceVideo.cancelVideoFrameCallback==="function")sourceVideo.cancelVideoFrameCallback(callbackId);
   resolve(chooseOptimalFps(mediaTimes))
  };
  const onFrame=(now,metadata)=>{
   if(Number.isFinite(metadata?.mediaTime))mediaTimes.push(metadata.mediaTime);
   if(performance.now()-startedAt>=800){finish();return}
   callbackId=sourceVideo.requestVideoFrameCallback(onFrame)
  };
  timer=setTimeout(finish,1500);
  try{callbackId=sourceVideo.requestVideoFrameCallback(onFrame)}catch{finish()}
 })
}
async function renderClip(){
 const{start,end}=range(),type=recorderType(),media=s.media,kind=s.kind,audio=s.audio,mode=$("audioMode").value,duration=end-start;
 const canvas=document.createElement("canvas");
 canvas.width=canvas.height=512;
 const paint=canvas.getContext("2d");
 if(!paint)throw Error("Não foi possível iniciar o canvas.");
 const audioContext=new AudioContext({latencyHint:"playback"});
 const audioOut=audioContext.createMediaStreamDestination();
 let sourceVideo,sourceImage,sourceAudio,videoStream,recorder,raf=0,timer=0;
 const chunks=[];
 try{
  if(kind==="video"){
   sourceVideo=document.createElement("video");
   sourceVideo.preload="auto";
   sourceVideo.playsInline=true;
   sourceVideo.muted=mode==="replace";
   const loaded=wait(sourceVideo,"loadedmetadata");
   sourceVideo.src=URL.createObjectURL(media);
   sourceVideo.load();
   await loaded;
   if(mode!=="replace"){
    const node=audioContext.createMediaElementSource(sourceVideo),gain=audioContext.createGain();
    gain.gain.value=mode==="mix"?.65:1;
    node.connect(gain).connect(audioOut)
   }
  }else{
   sourceImage=new Image();
   sourceImage.src=URL.createObjectURL(media);
   await sourceImage.decode()
  }
  if(audio&&mode!=="original"){
   sourceAudio=document.createElement("audio");
   sourceAudio.preload="auto";
   const loaded=wait(sourceAudio,"loadedmetadata").catch(()=>{});
   sourceAudio.src=URL.createObjectURL(audio);
   sourceAudio.load();
   await loaded;
   const node=audioContext.createMediaElementSource(sourceAudio),gain=audioContext.createGain();
   gain.gain.value=mode==="mix"&&kind==="video"?.8:1;
   node.connect(gain).connect(audioOut)
  }
  if(kind==="image"&&!audio)throw Error("Adicione um áudio para transformar uma imagem em clipe sonoro.");
  if(mode==="original"&&kind==="image")throw Error("Uma imagem estática precisa de áudio personalizado.");
  if(!audioOut.stream.getAudioTracks().length)throw Error("Selecione um áudio ou use o áudio original do vídeo.");

  let fps=configuredFps();
  if(sourceVideo){
   await seekVideo(sourceVideo,start);
   if(fps===null){
    sourceVideo.style.cssText="position:fixed;left:-2px;bottom:0;width:1px;height:1px;opacity:.01;pointer-events:none;z-index:-1";
    document.body.appendChild(sourceVideo);
    await audioContext.resume();
    await sourceVideo.play();
    fps=await estimatePresentedFps(sourceVideo);
    sourceVideo.pause();
    await seekVideo(sourceVideo,start)
   }
  }else if(fps===null){
   fps=5
  }
  videoStream=canvas.captureStream(fps);
  const stream=new MediaStream([...videoStream.getVideoTracks(),...audioOut.stream.getAudioTracks()]);
  recorder=new MediaRecorder(stream,type?{mimeType:type,videoBitsPerSecond:1200000,audioBitsPerSecond:96000}:undefined);
  const finished=new Promise((resolve,reject)=>{
   recorder.addEventListener("dataavailable",e=>{if(e.data?.size)chunks.push(e.data)});
   recorder.addEventListener("stop",resolve,{once:true});
   recorder.addEventListener("error",e=>reject(e.error||Error("Falha na gravação.")),{once:true})
  });
  if(sourceAudio)sourceAudio.currentTime=0;
  await audioContext.resume();
  recorder.start(250);
  if(sourceVideo)await sourceVideo.play();
  if(sourceAudio)await sourceAudio.play();
  const draw=()=>{
   paint.fillStyle="#151522";
   paint.fillRect(0,0,512,512);
   const frame=sourceVideo||sourceImage,w=sourceVideo?sourceVideo.videoWidth:sourceImage.naturalWidth,h=sourceVideo?sourceVideo.videoHeight:sourceImage.naturalHeight;
   if(frame&&w&&h){
    const scale=Math.min(512/w,512/h),dw=w*scale,dh=h*scale;
    paint.drawImage(frame,(512-dw)/2,(512-dh)/2,dw,dh)
   }
   if(recorder.state==="recording")raf=requestAnimationFrame(draw)
  };
  draw();
  await new Promise(resolve=>{timer=setTimeout(resolve,duration*1000)});
  if(sourceVideo)sourceVideo.pause();
  if(sourceAudio)sourceAudio.pause();
  if(raf)cancelAnimationFrame(raf);
  if(recorder.state!=="inactive")recorder.stop();
  await finished;
  const blob=new Blob(chunks,{type:recorder.mimeType||type||"video/webm"});
  if(!blob.size)throw Error("O navegador não gerou o vídeo.");
  return{blob,mimeType:blob.type,duration,start,end,audioMode:mode,fps,extension:blob.type.includes("mp4")?"mp4":"webm"}
 }finally{
  clearTimeout(timer);
  if(raf)cancelAnimationFrame(raf);
  for(const el of[sourceVideo,sourceAudio])if(el){
   el.pause();
   if(el.src.startsWith("blob:"))URL.revokeObjectURL(el.src);
   el.removeAttribute("src");
   if(el===sourceVideo)el.remove()
  }
  if(sourceImage?.src.startsWith("blob:"))URL.revokeObjectURL(sourceImage.src);
  videoStream?.getTracks().forEach(x=>x.stop());
  audioOut.stream.getTracks().forEach(x=>x.stop());
  await audioContext.close().catch(()=>{})
 }
}
function renderPack(){const list=$("packList");list.replaceChildren();if(!s.pack.length){const empty=document.createElement("div");empty.className="empty";empty.innerHTML="<span>♫</span><b>Seu pacote está vazio</b><small>Adicione pelo menos um clipe para começar.</small>";list.append(empty)}else s.pack.forEach((item,index)=>{const row=document.createElement("div");row.className="pack-item";const number=document.createElement("span");number.className="pack-number";number.textContent=String(index+1).padStart(2,"0");const copy=document.createElement("div");copy.className="pack-copy";const name=document.createElement("b");name.textContent=item.name;const meta=document.createElement("small");meta.textContent=t(item.duration)+" · "+item.fps+" FPS · "+size(item.blob.size)+" · "+item.extension.toUpperCase();copy.append(name,meta);const remove=document.createElement("button");remove.className="remove";remove.type="button";remove.textContent="×";remove.setAttribute("aria-label","Remover "+item.name);remove.addEventListener("click",()=>{s.pack.splice(index,1);renderPack();updateRange()});const send=document.createElement("button");send.className="share-clip";send.type="button";send.textContent="WhatsApp";send.setAttribute("aria-label","Enviar "+item.name+" ao WhatsApp");send.addEventListener("click",()=>shareClip(item));row.append(number,copy,send,remove);list.append(row)});$("packCount").textContent=s.pack.length+" de "+MAX_ITEMS+" clipes";$("exportPack").disabled=!s.pack.length}
async function addCurrent(){
 if(!s.media||s.rendering||s.pack.length>=MAX_ITEMS)return;
 const{start,end}=range();if(!isClipRangeAllowed(start,end,s.duration,MAX)){toast("O trecho deve estar entre 0,1 e 10 segundos e dentro da duração da mídia.",true);updateRange();return}
 const button=$("addToPack"),sourceMedia=s.media,sourceAudio=s.audio,sourceName=sourceMedia.name,audioName=sourceAudio?.name||"áudio original";
 s.rendering=true;button.textContent="Preparando clipe…";updateRange();
 try{
  const clip=await renderClip(),total=s.pack.reduce((sum,item)=>sum+item.blob.size,0);
  if(total+clip.blob.size>MAX_PACK_BYTES)throw Error("O pacote ultrapassa 75 MB. Exporte ou remova clipes antes de continuar.");
  const id=nextStickerId(s),n=s.nextId-1;
  s.pack.push({id,name:sourceName.replace(/\.[^.]+$/,"").slice(0,52)||"Sticker "+n,sourceName,audioName,...clip});
  renderPack();toast("Clipe adicionado ao pacote.")
 }catch(e){toast(e.message||"Não foi possível renderizar.",true)}
 finally{s.rendering=false;button.textContent="＋ Adicionar clipe ao pacote";updateRange()}
}
function blobBase64(blob){return new Promise((resolve,reject)=>{const r=new FileReader();r.onload=()=>{const v=String(r.result||""),i=v.indexOf(",");i<0?reject(Error("Falha ao preparar arquivo.")):resolve(v.slice(i+1))};r.onerror=()=>reject(r.error||Error("Falha ao ler arquivo."));r.readAsDataURL(blob)})}
async function deliver(blob,name,mime){if(window.AndroidBridge?.shareFile){window.AndroidBridge.shareFile(await blobBase64(blob),mime,name);return}const file=new File([blob],name,{type:mime,lastModified:Date.now()});if(navigator.share&&navigator.canShare?.({files:[file]})){try{await navigator.share({files:[file],title:name});return}catch(e){if(e.name==="AbortError")return}}const url=URL.createObjectURL(blob),a=document.createElement("a");a.href=url;a.download=name;a.rel="noopener";document.body.append(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),30000);toast("Arquivo preparado para download.")}
async function shareClip(item){try{const name=safeArchiveName(item.name,item.id)+"."+item.extension,encoded=await blobBase64(item.blob);if(window.AndroidBridge?.shareToWhatsApp){window.AndroidBridge.shareToWhatsApp(encoded,item.mimeType,name);return}await deliver(item.blob,name,item.mimeType)}catch(error){toast(error.message||"Não foi possível compartilhar o clipe.",true)}}
async function exportPack(){if(!s.pack.length)return;const button=$("exportPack");button.disabled=true;button.textContent="Montando ZIP…";try{const totalBytes=s.pack.reduce((sum,item)=>sum+item.blob.size,0);if(totalBytes>MAX_PACK_BYTES)throw Error("O pacote ultrapassa 75 MB.");const title=$("packName").value.trim()||"Meu pacote sonoro",items=s.pack.map(x=>({id:x.id,title:x.name,videoFile:"stickers/"+x.id+"."+x.extension,mimeType:x.mimeType,durationSeconds:Number(x.duration.toFixed(2)),fps:x.fps,originalMedia:x.sourceName,sourceAudio:x.audioName,audioMode:x.audioMode,trim:{startSeconds:x.start,endSeconds:x.end}}));const manifest={format:"erikraft-sound-stickers-pack",formatVersion:1,title,publisher:$("publisher").value.trim()||"ErikrafT",createdAt:new Date().toISOString(),processing:"local-browser",whatsappNativeImport:false,items};const instructions=[title,"Criado com ErikrafT Sound Stickers","","IMPORTANTE: este ZIP é um pacote de exportação e backup. O WhatsApp não oferece importação direta deste formato como pacote de figurinhas sonoras.","","COMO USAR:","1. Extraia os clipes da pasta stickers/.","2. No WhatsApp, abra o seletor de figurinhas e escolha Criar.","3. Selecione o MP4 e finalize a edição no criador nativo quando a função de som estiver disponível na sua conta.","4. Se a função não estiver liberada, o clipe pode ser compartilhado como vídeo comum.","","O formato de vídeo depende do navegador. Confira pack.json. Os arquivos foram processados localmente."].join("\n");const entries=[{name:"pack.json",data:JSON.stringify(manifest,null,2)},{name:"LEIA-ME.txt",data:instructions}];for(const x of s.pack)entries.push({name:"stickers/"+x.id+"."+x.extension,data:x.blob});await deliver(await createZip(entries),safeArchiveName(title)+".zip","application/zip");toast("Pacote exportado com "+s.pack.length+" clipe(s).")}catch(e){toast(e.message||"Falha ao exportar.",true)}finally{button.textContent="Exportar pacote ↓";button.disabled=!s.pack.length}}
$("mediaInput").addEventListener("change",async e=>{const f=e.target.files?.[0];if(!f)return;const token=++s.loadToken;try{const loaded=await loadMedia(f,token);if(loaded&&token===s.loadToken)toast("Mídia carregada. Adicione ao pacote.")}catch(error){if(token!==s.loadToken)return;clearMedia();toast(error.message,true)}});
$("audioInput").addEventListener("change",e=>{const f=e.target.files?.[0];if(!f)return;if((!f.type.startsWith("audio/")&&!/\.(mp3|m4a|aac|ogg|opus|wav|flac)$/i.test(f.name))||f.size>MAX_BYTES){toast("Selecione áudio compatível de até 150 MB.",true);return}s.audio=f;if(s.kind==="image")$("audioMode").value="replace";setPlayer();toast("Áudio personalizado carregado.")});$("audioMode").addEventListener("change",setPlayer);
$("clearMedia").addEventListener("click",clearMedia);$("trimStart").addEventListener("change",updateRange);$("trimEnd").addEventListener("change",updateRange);$("addToPack").addEventListener("click",addCurrent);$("exportPack").addEventListener("click",exportPack);
$("playAudio").addEventListener("click",()=>player.paused?player.play().catch(()=>toast("Toque novamente para iniciar o áudio.",true)):player.pause());
$("audioSeek").addEventListener("input",()=>{if(Number.isFinite(player.duration))player.currentTime=Number($("audioSeek").value)/100*player.duration});
$("volume").addEventListener("input",()=>{applyPreviewVolume();player.muted=Number($("volume").value)===0;if(mixedPreview)mixedPreview.muted=player.muted;syncPlayer()});$("muteAudio").addEventListener("click",()=>{player.muted=!player.muted;if(mixedPreview)mixedPreview.muted=player.muted;syncPlayer()});
["timeupdate","loadedmetadata","play","pause","ended","volumechange"].forEach(e=>player.addEventListener(e,syncPlayer));player.addEventListener("play",syncMixedPreviewPlayback);player.addEventListener("pause",()=>mixedPreview?.pause());player.addEventListener("seeking",syncMixedPreviewTime);player.addEventListener("timeupdate",syncMixedPreviewTime);player.addEventListener("ended",()=>mixedPreview?.pause());player.addEventListener("volumechange",()=>{if(mixedPreview)mixedPreview.muted=player.muted});
window.addEventListener("beforeunload",revoke);renderPack();drawWave();updateRange();
