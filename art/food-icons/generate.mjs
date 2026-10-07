import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';
import {createRequire} from 'node:module';
import {produce} from './produce.mjs';
import {fruits} from './fruit.mjs';
import {fungi} from './mushrooms.mjs';
import {proteins} from './protein.mjs';
import {seafood} from './seafood.mjs';
import {pantry} from './pantry.mjs';
import {refined} from './refined.mjs';
import {p,line,rect,leaf,group,ink,cream,green} from './drawing.mjs';

const here=path.dirname(fileURLToPath(import.meta.url)),root=path.resolve(here,'../..');
// The shipped catalog is the source of truth; private docs are not required.
const catalog=JSON.parse(fs.readFileSync(path.join(root,'app/src/main/assets/food_catalog.v1.json'),'utf8'));
const xmlDir=path.join(root,'app/src/main/res/drawable'),preview=path.join(here,'preview');
const families=[produce,fruits,fungi,proteins,seafood,pantry];
const moduleRoot=process.env.CODEX_WORKSPACE_NODE_MODULES||path.join(process.env.USERPROFILE,'.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules');
const sharp=createRequire(path.join(moduleRoot,'package.json'))('sharp');
const base=Object.assign({},...families),art={...base,...refined};
if(families.reduce((n,x)=>n+Object.keys(x).length,0)!==Object.keys(base).length)throw Error('重复的绘制定义');
for(const f of catalog.foods)if(!art[f.id])throw Error('缺少逐项设计：'+f.id);
for(const dir of [xmlDir,preview])fs.mkdirSync(dir,{recursive:true});
const esc=s=>String(s).replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;');
function svgNodes(nodes){return nodes.map(s=>s.children?`<g transform="translate(${s.x} ${s.y}) rotate(${s.angle}) scale(${s.sx} ${s.sy})">${svgNodes(s.children)}</g>`:`<path d="${s.d}" fill="${s.fill}" stroke="${s.stroke}" stroke-width="${s.width}" stroke-linecap="round" stroke-linejoin="round"/>`).join('');}
function xmlNodes(nodes,indent='  '){return nodes.map(s=>s.children?`${indent}<group android:translateX="${s.x}" android:translateY="${s.y}" android:scaleX="${s.sx}" android:scaleY="${s.sy}" android:rotation="${s.angle}">\n${xmlNodes(s.children,indent+'  ')}\n${indent}</group>`:`${indent}<path android:pathData="${s.d}" android:fillColor="${s.fill==='none'?'#00000000':s.fill}" android:strokeColor="${s.stroke==='none'?'#00000000':s.stroke}" android:strokeWidth="${s.width}" android:strokeLineCap="round" android:strokeLineJoin="round"/>`).join('\n');}
const svg=nodes=>`<?xml version="1.0" encoding="UTF-8"?>\n<svg xmlns="http://www.w3.org/2000/svg" width="96" height="96" viewBox="0 0 96 96">${svgNodes(nodes)}</svg>\n`;
const xml=nodes=>`<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="96dp" android:height="96dp" android:viewportWidth="96" android:viewportHeight="96">\n${xmlNodes(nodes)}\n</vector>\n`;
const countPaths=nodes=>nodes.reduce((n,s)=>n+(s.children?countPaths(s.children):1),0);
const entries=[],bodies={};
async function normalize(nodes){
  const overscan=`<svg xmlns="http://www.w3.org/2000/svg" width="640" height="640" viewBox="-32 -32 160 160">${svgNodes(nodes)}</svg>`;
  const {data,info}=await sharp(Buffer.from(overscan)).ensureAlpha().raw().toBuffer({resolveWithObject:true});
  let minX=640,minY=640,maxX=-1,maxY=-1;
  for(let y=0;y<info.height;y++)for(let x=0;x<info.width;x++)if(data[(y*info.width+x)*4+3]>5){minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);}
  if(maxX<0||minX===0||minY===0||maxX===639||maxY===639)throw Error('空图或原始绘图越过检测画布');
  const left=minX/4-32,top=minY/4-32,w=(maxX-minX+1)/4,h=(maxY-minY+1)/4;
  const k=+(80/Math.max(w,h)).toFixed(5),tx=+(48-(left+w/2)*k).toFixed(5),ty=+(48-(top+h/2)*k).toFixed(5);
  return [group(nodes,tx,ty,k)];
}
for(const food of catalog.foods){
  const design=art[food.id],nodes=await normalize(design.draw()),source=svg(nodes),android=xml(nodes);
  if(source.includes('NaN')||source.includes('undefined'))throw Error(food.id+' 路径含非法数值');
  fs.writeFileSync(path.join(here,food.iconKey+'.svg'),source);
  fs.writeFileSync(path.join(xmlDir,food.iconKey+'.xml'),android);
  bodies[food.id]=svgNodes(nodes);
  entries.push({id:food.id,name:food.name,categoryId:food.categoryId,iconKey:food.iconKey,iconBrief:food.iconBrief,source:`art/food-icons/${food.iconKey}.svg`,drawable:`app/src/main/res/drawable/${food.iconKey}.xml`,revision:3,family:design.family,designParameters:design.spec,pathCount:countPaths(nodes),status:'AUTHORED_REQUIRES_USER_VISUAL_ACCEPTANCE',sha256:crypto.createHash('sha256').update(source).digest('hex')});
}
const special={ic_food_custom:[rect(18,22,60,58,10,cream),leaf(48,64,30,32,30,green),line('M 48 43 V 69 M 36 56 H 60',ink,2.5)],
  ic_stat_freshness:[p('M 25 14 H 71 V 82 H 25 Z M 31 20 V 35 H 65 V 20 Z M 31 41 V 76 H 65 V 41 Z','#FFFFFF','none'),p('M 35 24 H 39 V 31 H 35 Z M 35 47 H 39 V 61 H 35 Z','#FFFFFF','none')]};
for(const[key,nodes]of Object.entries(special)){fs.writeFileSync(path.join(here,key+'.svg'),svg(nodes));fs.writeFileSync(path.join(xmlDir,key+'.xml'),xml(nodes));}
const header=(w,h)=>`<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}"><rect width="${w}" height="${h}" fill="#F4F6EE"/>`;
const label=(x,y,text,size=15)=>`<text x="${x}" y="${y}" text-anchor="middle" font-family="Microsoft YaHei, Noto Sans CJK SC, sans-serif" font-size="${size}" fill="#2A493E">${esc(text)}</text>`;
const inline=(id,x,y,size)=>`<svg x="${x}" y="${y}" width="${size}" height="${size}" viewBox="0 0 96 96">${bodies[id]}</svg>`;
for(const cat of catalog.categories){
  const items=entries.filter(e=>e.categoryId===cat.id),cols=5,cw=192,ch=160,w=cols*cw,h=Math.ceil(items.length/cols)*ch+72;
  let sheet=header(w,h)+label(w/2,36,`${cat.name} · ${items.length} 枚`,24);
  items.forEach((f,i)=>{const x=i%cols*cw,y=Math.floor(i/cols)*ch+62;sheet+=`<rect x="${x+6}" y="${y}" width="${cw-12}" height="${ch-10}" rx="18" fill="white"/>`+inline(f.id,x+42,y+6,108)+label(x+cw/2,y+132,f.name);});
  fs.writeFileSync(path.join(preview,cat.id+'.svg'),sheet+'</svg>');
}
const showcase=['pork_strips','pork_slices','pork_cubes','beef_strips','chicken_cubes','pork_belly','chicken_wing_mid','chicken_feet','fish_slices','fish_balls','spinach','bok_choy','shiitake','fresh_noodles','rice_cake','udon','dried_tofu_skin','vermicelli'];
let hero=header(1080,880)+label(540,42,'鲜序 · 更容易认出的食材',28)+label(540,72,'自然配色 / 清晰切面 / 小尺寸也能看清',15);
showcase.forEach((id,i)=>{const f=entries.find(e=>e.id===id),x=i%6*180,y=100+Math.floor(i/6)*254;hero+=`<rect x="${x+6}" y="${y}" width="168" height="240" rx="20" fill="white"/>`+inline(id,x+24,y+8,132)+label(x+90,y+164,f.name,17)+inline(id,x+40,y+178,48)+inline(id,x+107,y+187,32)+label(x+65,y+231,'48 px',10)+label(x+123,y+231,'32 px',10);});
fs.writeFileSync(path.join(preview,'showcase.svg'),hero+'</svg>');
const compare=['pork_strips','pork_cubes','pork_belly','chicken_tender','spinach','bok_choy','shiitake','fresh_noodles','broccoli','carrot','salmon','shrimp'];
const beforeDir=path.join(preview,'before'),available=compare.filter(id=>fs.existsSync(path.join(beforeDir,`food_${id}.svg`)));
if(available.length){
  let sheet=header(1080,850)+label(540,40,'原图标 → 优化后',28)+label(540,69,'造型、层次与小尺寸辨识度',15);
  available.forEach((id,i)=>{const f=entries.find(e=>e.id===id),x=i%4*270,y=92+Math.floor(i/4)*248;
    const old=fs.readFileSync(path.join(beforeDir,f.iconKey+'.svg'),'utf8').replace(/^[\s\S]*?<svg[^>]*>/,'').replace(/<\/svg>\s*$/,'');
    sheet+=`<rect x="${x+6}" y="${y}" width="258" height="234" rx="20" fill="white"/><svg x="${x+16}" y="${y+24}" width="108" height="108" viewBox="0 0 96 96">${old}</svg>`+inline(id,x+144,y+24,108)+label(x+136,y+86,'→',17)+label(x+135,y+157,f.name,17)+`<svg x="${x+48}" y="${y+173}" width="40" height="40" viewBox="0 0 96 96">${old}</svg>`+inline(id,x+177,y+169,48);
  });fs.writeFileSync(path.join(preview,'comparison.svg'),sheet+'</svg>');
}
const cards=entries.map(f=>`<article data-category="${f.categoryId}" data-search="${esc(f.name+' '+f.id)}"><div class="picture"><svg viewBox="0 0 96 96" role="img" aria-label="${esc(f.name)}">${bodies[f.id]}</svg></div><strong>${esc(f.name)}</strong><small>${f.iconKey}</small></article>`).join('');
fs.writeFileSync(path.join(preview,'index.html'),`<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>鲜序 · 食材图标</title><style>*{box-sizing:border-box}body{margin:0;background:#F4F6EE;color:#2A493E;font:15px 'Microsoft YaHei',system-ui}header{padding:32px;background:#254E40;color:#FAF7ED}h1{margin:0 0 12px}.toolbar{position:sticky;top:0;padding:16px 32px;display:flex;gap:16px;background:#F4F6EEF5;align-items:center}input,select{padding:10px;border:1px solid #CAD4C5;border-radius:10px;background:white}main{max-width:1280px;margin:24px auto;padding:0 24px;display:grid;grid-template-columns:repeat(auto-fill,minmax(164px,1fr));gap:16px}article{text-align:center;background:white;border-radius:20px;padding:16px}.picture{height:136px;display:grid;place-items:center}.picture svg{width:var(--size,112px);height:var(--size,112px)}strong,small{display:block}small{font-size:10px;color:#7C8C81;margin-top:8px}.hide-labels strong,.hide-labels small{visibility:hidden}</style><header><h1>鲜序 · 食材图标</h1><div>${entries.length} 种食材 · 自然配色，清晰轮廓</div></header><div class="toolbar"><input id="search" type="search" placeholder="搜索食材"><select id="category"><option value="">全部分类</option>${catalog.categories.map(c=>`<option value="${c.id}">${c.name}</option>`).join('')}</select><label>尺寸 <select id="size"><option>32</option><option>48</option><option>64</option><option selected>112</option></select></label><label><input id="labels" type="checkbox">隐藏名称</label><span id="count">${entries.length} 枚</span></div><main>${cards}</main><script>const cards=[...document.querySelectorAll('article')];function filter(){const q=document.querySelector('#search').value.toLowerCase(),c=document.querySelector('#category').value;let n=0;cards.forEach(a=>{a.hidden=!((!c||a.dataset.category===c)&&a.dataset.search.toLowerCase().includes(q));if(!a.hidden)n++;});document.querySelector('#count').textContent=n+' 枚';}document.querySelector('#search').addEventListener('input',filter);document.querySelector('#category').addEventListener('change',filter);document.querySelector('#size').addEventListener('change',e=>document.documentElement.style.setProperty('--size',e.target.value+'px'));document.querySelector('#labels').addEventListener('change',e=>document.body.classList.toggle('hide-labels',e.target.checked));</script></html>`);
fs.writeFileSync(path.join(here,'manifest.json'),JSON.stringify({schemaVersion:2,revision:3,catalogDataVersion:catalog.dataVersion,style:{viewBox:'0 0 96 96',outline:ink,background:'transparent',method:'Explicit food silhouettes, natural color planes and selective detail; SVG and Android paths are identical'},count:entries.length,items:entries},null,2)+'\n');
await import('./generate-registry.mjs');
console.log(JSON.stringify({foodSVG:entries.length,foodVector:entries.length,refinedDesigns:Object.keys(refined).length,uniqueSourceHashes:new Set(entries.map(e=>e.sha256)).size}));
