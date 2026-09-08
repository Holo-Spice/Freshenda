// 仅使用 SVG 与 VectorDrawable 都能表达的路径和仿射组。
export const ink='#203F36', cream='#F5EFDC', shade='#D9CFAD', green='#7D9C67', light='#B5C78C', dark='#4E7053', red='#C77459', pink='#D79B88', purple='#876681', brown='#9B7758', yellow='#E2BE68', orange='#DF9B59', blue='#91ABA7';
export const n = v => +v.toFixed(3);
export const p = (d, fill='none', stroke=ink, width=2.3) => ({d,fill,stroke,width});
export const line=(d,color=ink,width=1)=>p(d,'none',color,width);
export function ellipse(x,y,rx,ry,fill=cream,stroke=ink,width=2.3){return p(`M ${n(x-rx)} ${y} A ${rx} ${ry} 0 1 0 ${n(x+rx)} ${y} A ${rx} ${ry} 0 1 0 ${n(x-rx)} ${y} Z`,fill,stroke,width);}
export const dot=(x,y,r,color=ink)=>ellipse(n(x),n(y),r,r,color,'none',0);
export const rect=(x,y,w,h,r,fill,stroke=ink,width=2.3)=>p(`M ${x+r} ${y} H ${x+w-r} Q ${x+w} ${y} ${x+w} ${y+r} V ${y+h-r} Q ${x+w} ${y+h} ${x+w-r} ${y+h} H ${x+r} Q ${x} ${y+h} ${x} ${y+h-r} V ${y+r} Q ${x} ${y} ${x+r} ${y} Z`,fill,stroke,width);
export const group=(children,x=0,y=0,sx=1,sy=sx,angle=0)=>({children,x,y,sx,sy,angle});
export function scallop(cx,cy,rx,ry,count=14,depth=.07,fill=green){
  const pts=[]; for(let i=0;i<count*8;i++){const a=i*Math.PI*2/(count*8); const k=1-depth*(.5+.5*Math.cos(a*count));pts.push([cx+Math.cos(a)*rx*k,cy+Math.sin(a)*ry*k]);}
  return p('M '+pts.map(a=>a.map(n).join(' ')).join(' L ')+' Z',fill);
}
export function star(cx,cy,r,inner,count=5,fill=green,width=1.4){let d='';for(let i=0;i<count*2;i++){const a=-Math.PI/2+i*Math.PI/count,rr=i%2?inner:r;d+=(i?' L ':'M ')+n(cx+Math.cos(a)*rr)+' '+n(cy+Math.sin(a)*rr);}return p(d+' Z',fill,ink,width);}
export function hatch(x,y,count=3,step=4,color=brown){return Array.from({length:count},(_,i)=>line(`M ${x+i*step} ${y} l 3 -2`,color,.8));}
export function leaf(x,y,w,h,angle=0,fill=green,form='oval',veinColor=dark){
  let d;
  if(form==='heart') d='M 0 0 C -23 -9 -19 -25 -8 -24 Q -2 -24 0 -17 Q 8 -30 17 -20 C 26 -7 10 -2 0 0 Z';
  else if(form==='arrow')d='M 0 0 L -13 -4 L -8 -12 Q -9 -25 0 -36 Q 12 -20 8 -12 L 13 -4 Z';
  else if(form==='lobed')d='M 0 0 Q -9 -5 -11 -9 L -5 -12 Q -18 -15 -13 -20 L -6 -22 Q -17 -27 -9 -30 Q -8 -34 0 -39 Q 10 -31 8 -28 L 14 -25 L 7 -20 Q 17 -15 9 -12 L 5 -10 L 11 -6 Z';
  else if(form==='curly')d='M 0 0 Q -10 1 -9 -6 Q -18 -4 -14 -12 Q -23 -12 -16 -19 Q -23 -26 -14 -27 Q -17 -36 -8 -32 Q -5 -42 0 -36 Q 7 -41 10 -32 Q 21 -35 17 -26 Q 24 -23 16 -18 Q 22 -12 13 -11 Q 17 -4 9 -5 Q 9 2 0 0 Z';
  else if(form==='serrated')d='M 0 0 L -7 -5 L -5 -8 L -11 -12 L -8 -16 L -14 -21 L -9 -25 L -10 -29 L 0 -38 L 10 -29 L 9 -25 L 14 -21 L 8 -16 L 11 -12 L 5 -8 L 7 -5 Z';
  else d='M 0 0 C -18 -8 -16 -27 0 -36 C 17 -27 17 -9 0 0 Z';
  const heart=form==='heart';
  const v=[p(d,fill,ink,1.8),line(heart?'M 0 -2 Q 1 -10 0 -17':'M 0 -2 Q 1 -16 0 -31',veinColor,1.1)];
  for(let i=0;i<(heart?2:3);i++){const yy=-8-i*(heart?3:7);v.push(line(`M 0 ${yy} Q -5 ${yy-1} -8 ${yy-6} M 0 ${yy} Q 5 ${yy-1} 8 ${yy-6}`,veinColor,.75));}
  return group(v,x,y,w/27,h/36,angle);
}
export function cube(x,y,w=32,h=22,fill=cream,side=shade){return [p(`M ${x} ${y} l ${w*.63} ${-w*.3} l ${w*.65} ${w*.25} l ${-w*.65} ${w*.32} Z`,fill),p(`M ${x} ${y} l ${w*.63} ${w*.27} v ${h} l ${-w*.63} ${-w*.27} Z`,side),p(`M ${x+w*.63} ${y+w*.27} l ${w*.65} ${-w*.32} v ${h} l ${-w*.65} ${w*.32} Z`,fill)];}
export function dish(){return [ellipse(48,66,35,16,shade),ellipse(48,63,35,14,cream),ellipse(48,63,29,10,cream,shade,1)];}
export function bowl(contents=[],color=cream){return [p('M 15 50 Q 18 77 48 81 Q 77 77 81 50 Z',color),p('M 26 67 Q 46 78 68 68 L 64 75 Q 48 83 31 75 Z',shade,'none'),ellipse(48,50,33,11,cream),...contents,line('M 16 51 Q 48 64 80 51',ink,1.5)];}
export function droplet(x=78,y=75){return [p(`M ${x} ${y-9} Q ${x+10} ${y+3} ${x} ${y+6} Q ${x-10} ${y+3} ${x} ${y-9} Z`,'#A8C0C0',ink,1.3),line(`M ${x-2} ${y} q -2 3 1 3`,cream,1.2)];}
export function seeds(cx,cy,rx,ry,count=10,color=ink){return Array.from({length:count},(_,i)=>{const a=i*Math.PI*2/count;return ellipse(n(cx+Math.cos(a)*rx),n(cy+Math.sin(a)*ry),1,1.8,color,'none',0);});}
